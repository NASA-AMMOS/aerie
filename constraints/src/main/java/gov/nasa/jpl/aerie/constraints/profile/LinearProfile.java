package gov.nasa.jpl.aerie.constraints.profile;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;

public interface LinearProfile extends Profile<LinearEquation> {

  // CREATE

  static LinearProfile from(final double constant) {
    return Profile.from(new LinearEquation(Duration.ZERO, constant, 0))::stream;
  }

  static LinearProfile fromDiscrete(final Profile<SerializedValue> discrete) {
    return discrete.mapValues($ -> new LinearEquation($.asReal().orElseThrow(
        () -> new InputMismatchException("Discrete profile of non-real type cannot be converted to linear")
    )))::stream;
  }

  default LinearProfile times(final double coefficient) {
    return mapValues(eq -> new LinearEquation(eq.initialTime, coefficient * eq.initialValue, coefficient * eq.rate))::stream;
  }

  default LinearProfile plus(final Profile<LinearEquation> other) {
    return Profile.map2Values(
      this, other,
      BinaryOperation.combineDefaultEmpty(
        (left, right) -> {
          final var shiftedRight = right.shiftInitialTime(left.initialTime);
          return new LinearEquation(left.initialTime, left.initialValue + shiftedRight.initialValue, left.rate + right.rate);
        }
      )
    )::stream;
  }

  default LinearProfile rate() {
    return mapValues($ -> new LinearEquation($.rate))::stream;
  }

  private static Windows compare(
      final Profile<LinearEquation> left,
      final Profile<LinearEquation> right,
      final BiPredicate<Double, Double> predicate
  ) {
    return Profile.flatCompareValues(
        left, right,
        (l, r) -> LinearEquation.compare(
            l, r, predicate
        )
    );
  }

  @Override
  default Windows equalTo(Profile<LinearEquation> other) {
    return compare(this, other, Double::equals);
  }

  @Override
  default Windows notEqualTo(Profile<LinearEquation> other) {
    return compare(this, other, (l, r) -> !l.equals(r));
  }

  default Windows lessThan(final Profile<LinearEquation> other) {
    return compare(this, other, (l, r) -> l < r);
  }

  default Windows lessThanOrEqual(final Profile<LinearEquation> other) {
    return compare(this, other, (l, r) -> l <= r);
  }

  default Windows greaterThan(final Profile<LinearEquation> other) {
    return compare(this, other, (l, r) -> l > r);
  }

  default Windows greaterThanOrEqual(final Profile<LinearEquation> other) {
    return compare(this, other, (l, r) -> l >= r);
  }

  @Override
  default Windows allEdges() {
    return bounds -> {
      final AtomicReference<Optional<Segment<LinearEquation>>> atomicPrevious = new AtomicReference<>(Optional.empty());
      return this.stream(bounds).flatMap(
         currentSegment -> {
           final Optional<Boolean> leftEdge;


           final var previous = atomicPrevious.getAndSet(Optional.of(currentSegment));
           final var currentInterval = currentSegment.interval();

           if (previous.isPresent()) {
             final var previousSegment = previous.get();

             if (
                 Interval.meets(previousSegment.interval(), currentInterval)
                 && currentInterval.includesStart()
             ) {
               leftEdge = Optional.of(
                   previousSegment.value().valueAt(currentInterval.start) == currentSegment.value().valueAt(currentInterval.start)
               );
             } else {
               leftEdge = Optional.empty();
             }
           } else {
             if (currentInterval.start == bounds.start && currentInterval.startInclusivity == bounds.startInclusivity) {
               leftEdge = Optional.of(currentSegment.value().rate == 0);
             } else {
               leftEdge = Optional.empty();
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
                       currentSegment.value().rate == 0
                   )
               )
           );
         }
     )
     .filter(Optional::isPresent)
     .map(Optional::get);
    };
  }

  static LinearProfile fromSimulatedProfile(final List<ProfileSegment<RealDynamics>> simulatedProfile) {
    return fromProfileHelper(Duration.ZERO, simulatedProfile, Optional::of);
  }

  static LinearProfile fromExternalProfile(final Duration offsetFromPlanStart, final List<ProfileSegment<Optional<RealDynamics>>> externalProfile) {
    return fromProfileHelper(offsetFromPlanStart, externalProfile, $ -> $);
  }

  private static <T> LinearProfile fromProfileHelper(
      final Duration offsetFromPlanStart,
      final List<ProfileSegment<T>> profile,
      final Function<T, Optional<RealDynamics>> transform
  ) {
    return bounds -> {
      final AtomicReference<Duration> cursor = new AtomicReference<>(offsetFromPlanStart);
      return profile
          .stream()
          .map(
            segment -> {
              final var start = cursor.getAndAccumulate(segment.extent(), Duration::plus);
              return transform.apply(segemnt.dynamics()).map(
                  $ -> Segment.of(
                      Interval.betweenClosedOpen(start, start.plus(segment.extent())),
                      new LinearEquation(start, $.initial, $.rate)
                  )
              );
            }
          )
          .filter(Optional::isPresent)
          .map(Optional::get);
    };
  }
}
