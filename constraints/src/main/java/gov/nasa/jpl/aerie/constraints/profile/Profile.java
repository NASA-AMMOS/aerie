package gov.nasa.jpl.aerie.constraints.profile;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;

public interface Profile<V> {

  // READ: methods for viewing profile contents.

  /**
   * The primary method that Profile inheritors implement. Creates a stream of ordered segments inside the bounds.
   *
   * MUST GUARANTEE:
   * - the start and end bounds of each segment's interval is monotonically increasing (including inclusivity).
   * - all segment intervals are subsets of the bounds.
   *
   * DOES NOT GUARANTEE:
   * - intervals are non-overlapping
   * - consecutive segments have unequal values
   *
   * @param bounds Bounds of evaluation. Segments that touch the bounds are assumed to continue to infinity.
   * @return a stream of segments
   */
  Stream<Segment<V>> stream(final Interval bounds);

  /**
   * Perform the operations and reify the results into an Interval Map.
   *
   *
   *
   * @param bounds Bounds of evaluation. Segments that touch the bounds are assumed to continue to infinity.
   * @return
   */
  default IntervalMap<V> evaluate(final Interval bounds) {
    final var stream = this.stream(bounds);
    final var result = IntervalMap.<V>builder();
    stream.forEachOrdered(result::set);
    return result.build();
  }

  default IntervalMap<V> evaluate() {
    return evaluate(Interval.FOREVER);
  }

  default Stream<V> values(final Interval bounds) {
    return stream(bounds).map(Segment::value);
  }

  default Stream<Interval> intervals(final Interval bounds) {
    return stream(bounds).map(Segment::interval);
  }

  default Iterable<Segment<V>> iterable(final Interval bounds) {
    return () -> stream(bounds).iterator();
  }

  default Iterable<V> iterableValues(final Interval bounds) {
    return () -> values(bounds).iterator();
  }

  default Iterable<Interval> iterableIntervals(final Interval bounds) {
    return () -> intervals(bounds).iterator();
  }

  // CREATE: functions for creating new profiles.

  static <V> Profile<V> from(final V constant) {
    return bounds -> Stream.of(Segment.of(bounds, constant));
  }

  static <V> Profile<V> from(final Interval interval, final V constant) {
    return bounds -> Stream.of(Segment.of(Interval.intersect(bounds, interval), constant));
  }

  static <R> Profile<R> fromSimulatedProfile(final List<ProfileSegment<R>> simulatedProfile) {
    return fromProfileHelper(Duration.ZERO, simulatedProfile, Optional::of);
  }

  static <R> Profile<R> fromExternalProfile(final Duration offsetFromPlanStart, final List<ProfileSegment<Optional<R>>> externalProfile) {
    return fromProfileHelper(offsetFromPlanStart, externalProfile, $ -> $);
  }

  private static <V, R> Profile<R> fromProfileHelper(
      final Duration offsetFromPlanStart,
      final List<ProfileSegment<V>> profile,
      final Function<V, Optional<R>> transform
  ) {
    return bounds -> {
      final AtomicReference<Duration> cursor = new AtomicReference<>(offsetFromPlanStart);
      return profile
          .stream()
          .map(
            segment -> {
              final var start = cursor.getAndAccumulate(segment.extent(), Duration::plus);
              return transform.apply(segment.dynamics()).map(
                  $ -> Segment.of(
                      Interval.betweenClosedOpen(start, start.plus(segment.extent())),
                      $
                  )
              );
            }
          )
          .filter(Optional::isPresent)
          .map(Optional::get);
    };
  }

  // UPDATE: methods and functions for composing and operating on profiles.

  default Profile<V> set(final Profile<V> newProfile) {
    return map2Values(
        this, newProfile,
        BinaryOperation.combineDefaultIdentity((l, r) -> r)
    );
  }

  default Profile<V> set(final Interval interval, final V value) {
    return set(Profile.from(interval, value));
  }

  default Profile<V> assignGaps(final Profile<V> defaultProfile) {
    return defaultProfile.set(this);
  }

  default Profile<V> unset(final Interval unsetInterval) {
    return bounds -> this.stream(bounds)
      .flatMap(seg -> {
        final var currentInterval = seg.interval();
        final var currentValue = seg.value();
        return Interval.subtract(currentInterval, unsetInterval)
            .stream()
            .map($ -> Segment.of($, currentValue));
      });
  }

  default <Result> Profile<Result> mapValues(final Function<V, Result> transform) {
    return bounds -> this.stream(bounds).map($ -> $.mapValue(transform));
  }

  default <Result> Profile<Result> flatMapValues(final Function<V, Profile<Result>> transform) {
    return bounds -> mapValues(transform)
        .stream(bounds)
        .flatMap($ -> $.value().stream($.interval()));
  }

  static <Left, Right, Result> Profile<Result> map2Values(
      final Profile<Left> leftProfile,
      final Profile<Right> rightProfile,
      final BinaryOperation<Left, Right, Result> op
  ) {
    return bounds -> {
      final var leftIterator = leftProfile.stream(bounds).iterator();
      final var rightIterator = rightProfile.stream(bounds).iterator();

      return StreamSupport.stream(
          ((Iterable<Optional<Segment<Result>>>) () -> new Iterator<>() {
            private Optional<Segment<Left>> remainingLeftSegment = Optional.empty();
            private Optional<Segment<Right>> remainingRightSegment = Optional.empty();

            @Override
            public boolean hasNext() {
              return remainingLeftSegment.isPresent() || remainingRightSegment.isPresent()
                     || leftIterator.hasNext() || rightIterator.hasNext();
            }

            @Override
            public Optional<Segment<Result>> next() {
              final Optional<Segment<Left>> left;
              final Optional<Segment<Right>> right;

              if (remainingLeftSegment.isPresent()) {
                left = remainingLeftSegment;
                remainingLeftSegment = Optional.empty();
              } else if (leftIterator.hasNext()) {
                left = Optional.of(leftIterator.next());
              } else {
                left = Optional.empty();
              }
              if (remainingRightSegment.isPresent()) {
                right = remainingRightSegment;
                remainingRightSegment = Optional.empty();
              } else if (rightIterator.hasNext()) {
                right = Optional.of(rightIterator.next());
              } else {
                right = Optional.empty();
              }

              if (left.isEmpty()) {
                return Segment.transpose(right.get().mapValue(op::right));
              } else if (right.isEmpty()) {
                return Segment.transpose(left.get().mapValue(op::left));
              } else {
                final var leftSegment = left.get();
                final var rightSegment = right.get();
                final var leftInterval = leftSegment.interval();
                final var rightInterval = rightSegment.interval();

                final BinaryOperation.OpMode opMode;

                if (leftInterval.start.shorterThan(rightInterval.start)) opMode = BinaryOperation.OpMode.Left;
                else if (rightInterval.start.shorterThan(leftInterval.start)) opMode = BinaryOperation.OpMode.Right;
                else if (rightInterval.startInclusivity.moreRestrictiveThan(leftInterval.startInclusivity)) opMode = BinaryOperation.OpMode.Left;
                else if (leftInterval.startInclusivity.moreRestrictiveThan(rightInterval.startInclusivity)) opMode = BinaryOperation.OpMode.Right;
                else opMode = BinaryOperation.OpMode.Combine;

                final var intersection = Interval.intersect(leftInterval, rightInterval);

                switch (opMode) {
                  case Left -> {
                    if (!intersection.isEmpty()) {
                      remainingLeftSegment = Optional.of(
                          leftSegment.mapInterval($ -> Interval.between(
                              intersection.start,
                              intersection.startInclusivity,
                              $.end,
                              $.endInclusivity))
                      );
                    }
                    remainingRightSegment = Optional.of(rightSegment);

                    if (!intersection.isEmpty()) {
                      return Segment.transpose(
                          leftSegment
                              .mapInterval($ -> Interval.between(
                                  $.start,
                                  $.startInclusivity,
                                  intersection.start,
                                  intersection.startInclusivity.opposite()))
                              .mapValue(op::left)
                      );
                    } else {
                      return Segment.transpose(leftSegment.mapValue(op::left));
                    }
                  }
                  case Right -> {
                    if (!intersection.isEmpty()) {
                      remainingRightSegment = Optional.of(
                          rightSegment.mapInterval($ -> Interval.between(
                              intersection.start,
                              intersection.startInclusivity,
                              $.end,
                              $.endInclusivity))
                      );
                    }
                    remainingLeftSegment = Optional.of(leftSegment);

                    if (!intersection.isEmpty()) {
                      return Segment.transpose(
                          rightSegment
                              .mapInterval($ -> Interval.between(
                                  $.start,
                                  $.startInclusivity,
                                  intersection.start,
                                  intersection.startInclusivity.opposite()))
                              .mapValue(op::right)
                      );
                    } else {
                      return Segment.transpose(rightSegment.mapValue(op::right));
                    }
                  }
                  default -> {
                    if (leftInterval.end.longerThan(intersection.end)
                        || intersection.endInclusivity.moreRestrictiveThan(leftInterval.endInclusivity)) {
                      remainingLeftSegment = Optional.of(
                          leftSegment.mapInterval($ -> Interval.between(
                              intersection.end,
                              intersection.endInclusivity.opposite(),
                              $.end,
                              $.endInclusivity))
                      );
                    } else if (rightInterval.end.longerThan(intersection.end)
                               || intersection.endInclusivity.moreRestrictiveThan(rightInterval.endInclusivity)) {
                      remainingRightSegment = Optional.of(
                          rightSegment.mapInterval($ -> Interval.between(
                              intersection.end,
                              intersection.endInclusivity.opposite(),
                              $.end,
                              $.endInclusivity))
                      );
                    }

                    return Segment.transpose(
                        Segment.of(intersection, op.combine(leftSegment.value(), rightSegment.value()))
                    );
                  }
                }
              }
            }
          }
        ).spliterator(), false)
          .filter(Optional::isPresent)
          .map(Optional::get);
    };
  }

  static <Left, Right, Result> Profile<Result> flatMap2Values(
      final Profile<Left> leftProfile,
      final Profile<Right> rightProfile,
      final BinaryOperation<Left, Right, Profile<Result>> op
  ) {
    return bounds -> map2Values(
        leftProfile, rightProfile,
        op
    )
        .stream(bounds)
        .flatMap($ -> $.value().stream($.interval()));
  }

  static <Left, Right> Windows compareValues(
      final Profile<Left> left,
      final Profile<Right> right,
      final BiPredicate<Left, Right> predicate
  ) {
    return map2Values(
        left, right,
        BinaryOperation.combineDefaultEmpty(predicate::test)
    )::stream;
  }

  static <Left, Right> Windows flatCompareValues(
      final Profile<Left> left,
      final Profile<Right> right,
      final BiFunction<Left, Right, Profile<Boolean>> predicate
  ) {
    return flatMap2Values(
        left, right,
        BinaryOperation.combineDefaultEmpty(predicate)
    )::stream;
  }

  default Profile<V> filterValues(final Predicate<V> predicate) {
    return bounds -> this.stream(bounds).filter($ -> predicate.test($.value()));
  }

  default Profile<V> filterIntervals(final Predicate<Interval> predicate) {
    return bounds -> this.stream(bounds).filter($ -> predicate.test($.interval()));
  }

  default Windows equalTo(Profile<V> other) {
    return compareValues(this, other, V::equals);
  }

  default Windows notEqualTo(Profile<V> other) {
    return compareValues(this, other, (l, r) -> !l.equals(r));
  }

  default Windows edges(final BinaryOperation<V, V, Boolean> edgeFilter) {
    return bounds -> {
      final AtomicReference<Optional<Segment<V>>> atomicPrevious = new AtomicReference<>(Optional.empty());
      return this.stream(bounds).flatMap(
        currentSegment -> {
          final Optional<Boolean> leftEdge;
          final Optional<Boolean> rightEdge;


          final var previous = atomicPrevious.getAndSet(Optional.of(currentSegment));
          final var currentInterval = currentSegment.interval();

          if (currentInterval.end == bounds.end && currentInterval.endInclusivity == bounds.endInclusivity) {
            if (bounds.includesEnd()) rightEdge = Optional.of(false);
            else rightEdge = Optional.empty();
          } else {
            rightEdge = edgeFilter.left(currentSegment.value());
          }

          if (previous.isPresent()) {
            final var previousSegment = previous.get();

            if (Interval.meets(previousSegment.interval(), currentInterval)) {
              leftEdge = edgeFilter.combine(previousSegment.value(), currentSegment.value());
            } else {
              leftEdge = edgeFilter.right(currentSegment.value());
            }
          } else {
            if (currentInterval.start == bounds.start && currentInterval.startInclusivity == bounds.startInclusivity) {
              if (bounds.includesStart()) leftEdge = Optional.of(false);
              else leftEdge = Optional.empty();
            } else {
              leftEdge = edgeFilter.right(currentSegment.value());
            }
          }

          return Stream.of(
            Segment.transpose(
              Segment.of(
                Interval.at(currentInterval.start),
                leftEdge
              )
            ),
            Optional.of(
              Segment.of(
                Interval.between(currentInterval.start, Exclusive, currentInterval.end, Exclusive),
                false
              )
            ),
            Segment.transpose(
              Segment.of(
                Interval.at(currentInterval.end),
                rightEdge
              )
            )
          );
        }
      )
        .filter(Optional::isPresent)
        .map(Optional::get);
    };
  }

  default Windows allEdges() {
    return edges(BinaryOperation.combineDefaultEmpty((l, r) -> !l.equals(r)));
  }

  default Windows specificEdges(final V from, final V to) {
    return edges(
        BinaryOperation.fromCases(
            $ -> $.equals(from) ? Optional.empty() : Optional.of(false),
            $ -> $.equals(to) ? Optional.empty() : Optional.of(false),
            (l, r) -> Optional.of(l.equals(from) && r.equals(to))
        )
    );
  }
}
