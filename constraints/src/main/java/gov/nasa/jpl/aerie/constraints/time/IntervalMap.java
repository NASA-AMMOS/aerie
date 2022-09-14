package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;

public final class IntervalMap<V> implements Iterable<Segment<V>> {
  // INVARIANT: `segments` is list of non-empty, non-overlapping segments in ascending order.
  // INVARIANT: If two adjacent segments abut exactly (e.g. [0, 3), [3, 5]), their values are non-equal.
  private final List<Segment<V>> segments;

  // PRECONDITION: The list of `segments` meets the invariants of the class.
  private IntervalMap(final List<Segment<V>> segments) {
    this.segments = Collections.unmodifiableList(segments);
  }

  public static <V> Builder<V> builder() {
    return new Builder<>();
  }

  public static <V> IntervalMap<V> of(final List<Segment<V>> segments) {
    final var builder = new Builder<V>(segments.size());
    for (final var segment : segments) {
      builder.add(segment.interval(), segment.value());
    }

    return builder.build();
  }

  public static <V> IntervalMap<V> of(final Interval interval, final V value) {
    return IntervalMap.of(List.of(Segment.of(interval, value)));
  }

  @SafeVarargs
  public static <V> IntervalMap<V> of(final Segment<V>... segments) {
    return IntervalMap.of(Arrays.asList(segments));
  }

  public static <V> IntervalMap<V> of(final List<Interval> intervals, final V value) {
    return IntervalMap.of(intervals.stream().map($ -> Segment.of($, value)).toList());
  }

  public static <V> IntervalMap<V> of(final V value) {
   return IntervalMap.of(List.of(Segment.of(Interval.FOREVER, value)));
  }


  public IntervalMap<V> set(final Interval interval, final V value) {
    return set(IntervalMap.of(interval, value));
  }

  public IntervalMap<V> set(final List<Interval> intervals, final V value) {
    return set(IntervalMap.of(intervals, value));
  }

  public IntervalMap<V> set(final IntervalMap<V> other) {
    return IntervalMap.map2(
        this, other,
        (a, b) -> (b.isPresent()) ? b : a
    );
  }


  public IntervalMap<V> unset(final Interval interval) {
    return this.unset(IntervalMap.of(interval, new Object()));
  }

  public IntervalMap<V> unset(final List<Interval> intervals) {
    return this.unset(IntervalMap.of(intervals, new Object()));
  }

  public IntervalMap<V> unset(final IntervalMap<?> other) {
    return IntervalMap.map2(
        this, other,
        (l, r) -> r.isPresent() ? Optional.empty() : l
    );
  }


  public IntervalMap<V> select(final Interval bounds) {
    return this.select(IntervalMap.of(bounds, new Object()));
  }

  public IntervalMap<V> select(final List<Interval> intervals) {
    return this.select(IntervalMap.of(intervals, new Object()));
  }

  public IntervalMap<V> select(final IntervalMap<?> other) {
    return IntervalMap.map2(
        this, other,
        (l, r) -> r.isPresent() ? l : Optional.empty()
    );
  }

  /**
   * Maps intervals and the gaps between them in IntervalMap intervals to new values following some function transform
   *  which converts the old values (or nulls, in the case of gaps) to new values.
   *
   * @param transform the function to apply to each value of intervals (or the gaps between!), mapping its original value type V to another value type R
   * @return a new IntervalMap with newly mapped values
   * @param <R> The new value type that the returned IntervalMap's intervals should correspond to
   */
  public <R> IntervalMap<R> map(final Function<V, R> transform) {
    return this.map((v, i) -> transform.apply(v));
  }

  public <R> IntervalMap<R> map(final BiFunction<V, Interval, R> transform) {
    final var builder = IntervalMap.<R>builder();

    for (final var segment : this.segments) {
      builder.add(segment.interval(), transform.apply(segment.value(), segment.interval()));
    }

    return builder.build();
  }

  public static <V1, V2, R>
  IntervalMap<R> map2(
      final IntervalMap<V1> left,
      final IntervalMap<V2> right,
      final BiFunction<Optional<V1>, Optional<V2>, Optional<R>> transform
  ) {
    final var result = new ArrayList<Segment<R>>();

    var startTime = Duration.MIN_VALUE;
    var startInclusivity = Inclusive;
    Duration endTime;
    Interval.Inclusivity endInclusivity;

    var leftIndex = 0;
    var rightIndex = 0;
    var nextLeftIndex = 0;
    var nextRightIndex = 0;

    Interval leftInterval;
    Interval rightInterval;
    Optional<V1> leftValue;
    Optional<V2> rightValue;

    Optional<R> previousValue = Optional.empty();

    while (startTime.shorterThan(Duration.MAX_VALUE) || startInclusivity == Inclusive) {
      if (leftIndex < left.size()) {
        var leftNextDefinedSegment = left.get(leftIndex);
        if (leftNextDefinedSegment.interval().start.shorterThan(startTime) || (leftNextDefinedSegment.interval().start.isEqualTo(startTime) && !leftNextDefinedSegment.interval().startInclusivity.moreRestrictiveThan(startInclusivity))) {
          leftInterval = leftNextDefinedSegment.interval();
          leftValue = Optional.of(leftNextDefinedSegment.value());
          nextLeftIndex = leftIndex + 1;
        } else {
          leftInterval = Interval.between(
              Duration.MIN_VALUE,
              Inclusive,
              leftNextDefinedSegment.interval().start,
              leftNextDefinedSegment.interval().startInclusivity.opposite());
          leftValue = Optional.empty();
        }
      } else {
        leftInterval = Interval.FOREVER;
        leftValue = Optional.empty();
      }

      if (rightIndex < right.size()) {
        var rightNextDefinedSegment = right.get(rightIndex);
        if (rightNextDefinedSegment.interval().start.shorterThan(startTime) || (rightNextDefinedSegment.interval().start.isEqualTo(startTime) && !rightNextDefinedSegment.interval().startInclusivity.moreRestrictiveThan(startInclusivity))) {
          rightInterval = rightNextDefinedSegment.interval();
          rightValue = Optional.of(rightNextDefinedSegment.value());
          nextRightIndex = rightIndex + 1;
        } else {
          rightInterval = Interval.between(
              Duration.MIN_VALUE,
              Inclusive,
              rightNextDefinedSegment.interval().start,
              rightNextDefinedSegment.interval().startInclusivity.opposite());
          rightValue = Optional.empty();
        }
      } else {
        rightInterval = Interval.FOREVER;
        rightValue = Optional.empty();
      }

      if (leftInterval.end.isEqualTo(rightInterval.end)) {
        endTime = leftInterval.end;
        if (leftInterval.includesEnd() && rightInterval.includesEnd()) {
          endInclusivity = Inclusive;
          leftIndex = nextLeftIndex;
          rightIndex = nextRightIndex;
        } else if (leftInterval.includesEnd()) {
          endInclusivity = Exclusive;
          rightIndex = nextRightIndex;
        } else if (rightInterval.includesEnd()) {
          endInclusivity = Exclusive;
          leftIndex = nextLeftIndex;
        } else {
          endInclusivity = Exclusive;
          rightIndex = nextRightIndex;
        }
      } else if (leftInterval.end.shorterThan(rightInterval.end)) {
        endTime = leftInterval.end;
        endInclusivity = leftInterval.endInclusivity;
        leftIndex = nextLeftIndex;
      } else {
        endTime = rightInterval.end;
        endInclusivity = rightInterval.endInclusivity;
        rightIndex = nextRightIndex;
      }
      var finalInterval = Interval.between(startTime, startInclusivity, endTime, endInclusivity);
      if (finalInterval.isEmpty()) continue;

      var newValue = transform.apply(leftValue, rightValue);
      if (newValue.isPresent()) {
        if (!newValue.equals(previousValue)) {
          result.add(Segment.of(finalInterval, newValue.get()));
        } else {
          var previousInterval = result.remove(result.size() - 1).interval();
          result.add(
              Segment.of(
                  Interval.unify(previousInterval, finalInterval),
                  newValue.get()
              )
          );
        }
      }
      previousValue = newValue;
      startTime = endTime;
      startInclusivity = endInclusivity.opposite();
    }

    // SAFETY: ???
    return new IntervalMap<>(result);
  }

  public IntervalMap<V> shiftBy(final Duration delta) {
    final var builder = IntervalMap.<V>builder();

    for (final var segment : this.segments) {
      final var interval = segment.interval();
      builder.add(
          Interval.between(
              interval.start.plus(delta), interval.startInclusivity,
              interval.end.plus(delta), interval.endInclusivity),
          segment.value());
    }

    return builder.build();
  }

  /**
   * The left bound of the leftmost defined interval.
   * @return the leftmost bound and its inclusivity
   */
  public Optional<Pair<Duration, Interval.Inclusivity>> minValidTimePoint() {
    if (this.isEmpty()) return Optional.empty();

    final var window = this.segments.get(0).interval();
    return Optional.of(Pair.of(window.start, window.startInclusivity));
  }

  /**
   * The right bound of the rightmost defined interval.
   * @return the rightmost bound and its inclusivity
   */
  public Optional<Pair<Duration, Interval.Inclusivity>> maxValidTimePoint() {
    if (this.isEmpty()) return Optional.empty();

    final var window = this.segments.get(this.segments.size() - 1).interval();
    return Optional.of(Pair.of(window.end, window.endInclusivity));
  }

  public Segment<V> get(final int index) {
    final var i = (index >= 0) ? index : this.segments.size() - index;
    return this.segments.get(i);
  }

  public int size() {
    return this.segments.size();
  }

  public boolean isEmpty() {
    return this.segments.isEmpty();
  }

  @Override
  public Iterator<Segment<V>> iterator() {
    return this.segments.iterator();
  }

  public Iterable<Interval> iterateEqualTo(final V value) {
    return () -> this.segments
        .stream()
        .filter($ -> $.value().equals(value))
        .map(Segment::interval)
        .iterator();
  }

  public Stream<Segment<V>> stream() {
    return StreamSupport.stream(this.spliterator(), false);
  }

  public boolean isAllEqualTo(final V value) {
    return this.stream().map($ -> $.value()).allMatch(value::equals);
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof final IntervalMap<?> o)) return false;
    return this.segments.equals(o.segments);
  }

  @Override
  public String toString() {
    return this.segments.toString();
  }


  public static final class Builder<V> {
    // INVARIANT: `segments` is list of non-empty, non-overlapping segments in ascending order.
    // INVARIANT: If two adjacent segments abut exactly (e.g. [0, 3), [3, 5]), their values are non-equal.
    private List<Segment<V>> segments;
    private boolean built = false;

    public Builder() {
      this.segments = new ArrayList<>();
    }

    public Builder(int initialCapacity) {
      this.segments = new ArrayList<>(initialCapacity);
    }

    public Builder<V> add(Interval interval, final V value) {
      if (this.built) throw new IllegalStateException();

      if (interval.isEmpty()) return this;

      // <> is `interval`, the interval to apply; [] is the currently-indexed interval in the map.
      // Cases: --[---]---<--->--
      int index = 0;
      while (index < this.segments.size() && IntervalAlgebra.endsStrictlyBefore(this.getInterval(index), interval)) {
        index += 1;
      }

      // Cases: --[---<---]--->-- and --[---<--->---]--
      if (index < this.segments.size() && IntervalAlgebra.startsBefore(this.getInterval(index), interval)) {
        // If the intervals agree on their value, we can unify the old interval with the new one.
        // Otherwise, we'll snip the old one.
        if (Objects.equals(this.getValue(index), value)) {
          interval = IntervalAlgebra.unify(this.segments.remove(index).interval(), interval);
        } else {
          final var prefix = IntervalAlgebra.intersect(this.getInterval(index), IntervalAlgebra.lowerBoundsOf(interval));
          final var suffix = IntervalAlgebra.intersect(this.getInterval(index), IntervalAlgebra.upperBoundsOf(interval));

          this.segments.set(index, Segment.of(prefix, this.getValue(index)));
          if (!IntervalAlgebra.isEmpty(suffix)) this.segments.add(index + 1, Segment.of(suffix, this.getValue(index)));

          index += 1;
        }
      }

      // Cases: --<---[---]--->--
      while (index < this.segments.size() && !IntervalAlgebra.endsAfter(this.getInterval(index), interval)) {
        this.segments.remove(index);
      }

      // Cases: --<---[--->---]--
      if (index < this.segments.size() && !IntervalAlgebra.startsStrictlyAfter(this.getInterval(index), interval)) {
        // If the intervals agree on their value, we can unify the old interval with the new one.
        // Otherwise, we'll snip the old one.
        if (Objects.equals(this.getValue(index), value)) {
          interval = IntervalAlgebra.unify(this.segments.remove(index).interval(), interval);
        } else {
          final var suffix = IntervalAlgebra.intersect(this.getInterval(index), IntervalAlgebra.upperBoundsOf(interval));

          this.segments.set(index, Segment.of(suffix, this.getValue(index)));
        }
      }

      // now, everything left of `index` is strictly left of `interval`,
      // and everything right of `index` is strictly right of `interval`,
      // so adding this interval to the list is trivial.
      this.segments.add(index, Segment.of(interval, value));

      return this;
    }

    public IntervalMap<V> build() {
      if (this.built) throw new IllegalStateException();
      this.built = true;

      final var segments = this.segments;
      this.segments = null;

      // SAFETY: `segments` meets the same invariants as required by `IntervalMap`.
      return new IntervalMap<>(segments);
    }

    private Interval getInterval(final int index) {
      return this.segments.get(index).interval();
    }

    private V getValue(final int index) {
      return this.segments.get(index).value();
    }
  }
}
