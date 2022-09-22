package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

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

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;

/**
 * A generic container that maps non-overlapping intervals on the timeline to values.
 *
 * Currently, this is only used to represent Windows (booleans), but can be used to represent
 * any type of profile.
 *
 * Gaps in the timeline that have no associated value are allowed, and are often referred to as
 * `null`; although admittedly they are more analogous to `void`. It is not recommended to explicitly store `null`
 * values in the map, although there is technically no reason why you couldn't.
 *
 * The meaning of a gap is typically interpreted to mean "unknown" rather than "undefined".
 *
 * @param <V> Type of data associated with each interval
 */
public final class IntervalMap<V> implements Iterable<Segment<V>> {
  // INVARIANT: `segments` is list of non-empty, non-overlapping segments in ascending order.
  // INVARIANT: If two adjacent segments abut exactly (e.g. [0, 3), [3, 5]), their values are non-equal.
  private final List<Segment<V>> segments;

  // PRECONDITION: The list of `segments` meets the invariants of the class.
  private IntervalMap(final List<Segment<V>> segments) {
    this.segments = Collections.unmodifiableList(segments);
  }

  /** Creates an IntervalMap builder */
  public static <V> Builder<V> builder() {
    return new Builder<>();
  }

  /**
   * Creates an IntervalMap from a potentially un-ordered or overlapping list of segments.
   *
   * Segments are ordered. In the case of overlapping segments, the latter value in the list
   * overwrites the former.
   */
  public static <V> IntervalMap<V> of(final List<Segment<V>> segments) {
    final var builder = new Builder<V>(segments.size());
    for (final var segment : segments) {
      builder.set(segment.interval(), segment.value());
    }

    return builder.build();
  }

  /** Creates an IntervalMap with a single segment. */
  public static <V> IntervalMap<V> of(final Interval interval, final V value) {
    return IntervalMap.of(List.of(Segment.of(interval, value)));
  }

  /**
   * Creates an IntervalMap from a potentially un-ordered or overlapping list of segments.
   *
   * Delegates to {@link IntervalMap#of(List)}.
   */
  @SafeVarargs
  public static <V> IntervalMap<V> of(final Segment<V>... segments) {
    return IntervalMap.of(Arrays.asList(segments));
  }

  /**
   * Creates an IntervalMap with many intervals all associated with the same value.
   *
   * Delegates to {@link IntervalMap#of(List)}.
   */
  public static <V> IntervalMap<V> of(final List<Interval> intervals, final V value) {
    return IntervalMap.of(intervals.stream().map($ -> Segment.of($, value)).toList());
  }

  /** Creates an IntervalMap that is equal to a single value for all representable time */
  public static <V> IntervalMap<V> of(final V value) {
   return IntervalMap.of(List.of(Segment.of(Interval.FOREVER, value)));
  }


  /**
   * Sets an interval to a value
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> set(final Interval interval, final V value) {
    return set(IntervalMap.of(interval, value));
  }

  /**
   * Sets a list of Intervals to a single value
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> set(final List<Interval> intervals, final V value) {
    return set(IntervalMap.of(intervals, value));
  }

  /**
   * Copys an IntervalMap on top of this.
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> set(final IntervalMap<V> other) {
    return IntervalMap.map2(
        this, other,
        (a, b) -> (b.isPresent()) ? b : a
    );
  }

  /**
   * Unsets the given intervals.
   *
   * Turns the given intervals into gaps, if they are not already.
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> unset(final Interval... intervals) {
    return this.unset(Arrays.stream(intervals).toList());
  }

  /**
   * Unsets the given intervals.
   *
   * Turns the given intervals into gaps, if they are not already.
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> unset(final List<Interval> intervals) {
    return map2(this, IntervalMap.of(intervals, new Object()), (l, r) -> r.isEmpty() ? l : Optional.empty());
  }

  /**
   * Unsets everything outside the given intervals.
   * @return a new IntervalMap
   */
  public IntervalMap<V> select(final Interval... intervals) {
    return select(Arrays.stream(intervals).toList());
  }

  /**
   * Unsets everything outside the given intervals.
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> select(final List<Interval> intervals) {
    return IntervalMap.map2(
        this, IntervalMap.of(intervals, new Object()),
        (l, r) -> r.isPresent() ? l : Optional.empty()
    );
  }

  /**
   * Maps intervals and the gaps between them in IntervalMap intervals to new values following some function transform
   *  which converts the old values and gaps to new values.
   *
   * @param transform a function which transforms an {@link Optional} to an Optional, where {@link Optional#empty()} corresponds
   *                  to a gap.
   * @return a new IntervalMap with newly mapped values
   * @param <R> The new value type that the returned IntervalMap's intervals should correspond to
   */
  public <R> IntervalMap<R> map(final Function<V, R> transform) {
    return this.map((v, i) -> transform.apply(v));
  }

  /**
   * Maps intervals and the gaps between them in IntervalMap intervals to new values following some function transform
   * which converts the old values and gaps to new values.
   *
   * The intervals can also be provided to the transform for inspection, but not modification.
   *
   * @param transform a function which transforms an {@link Optional} and {@link Interval} to an Optional, where {@link Optional#empty()} corresponds
   *                  to a gap.
   * @return a new IntervalMap with newly mapped values
   * @param <R> The new value type that the returned IntervalMap's intervals should correspond to
   */
  public <R> IntervalMap<R> map(final BiFunction<V, Interval, R> transform) {
    final var builder = IntervalMap.<R>builder();

    for (final var segment : this.segments) {
      builder.set(segment.interval(), transform.apply(segment.value(), segment.interval()));
    }

    return builder.build();
  }

  /**
   * A generalized binary operation between two IntervalMaps.
   *
   * @param left left operand
   * @param right right operand
   * @param transform a function that transforms two {@link Optional}s of the left and right operands' types to an
   *                  optional of a new type.
   * @param <V1> value type of the left operand
   * @param <V2> value type of the right operand
   * @param <R> value type of the result
   * @return a new IntervalMap, the result of applying the transform
   */
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

  /** Gets the segment at a given index */
  public Segment<V> get(final int index) {
    final var i = (index >= 0) ? index : this.segments.size() + index;
    return this.segments.get(i);
  }

  /** The number of defined intervals in this. */
  public int size() {
    return this.segments.size();
  }

  /** Whether this has no defined segments */
  public boolean isEmpty() {
    return this.segments.isEmpty();
  }

  @Override
  public Iterator<Segment<V>> iterator() {
    return this.segments.iterator();
  }

  /** Creates an iterable over the Intervals where this map is equal to a value */
  public Iterable<Interval> iterateEqualTo(final V value) {
    return () -> this.segments
        .stream()
        .filter($ -> $.value().equals(value))
        .map(Segment::interval)
        .iterator();
  }

  public Stream<Segment<V>> stream() {
    return this.segments.stream();
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


  /** A builder for IntervalMap */
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

    public Builder<V> set(final IntervalMap<V> map) {
      for (final var segment: map) {
        set(segment);
      }
      return this;
    }

    public Builder<V> set(final Segment<V> segment) {
      return set(segment.interval(), segment.value());
    }

    public Builder<V> set(Interval interval, final V value) {
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
          final var prefix = IntervalAlgebra.intersect(this.getInterval(index), IntervalAlgebra.strictLowerBoundsOf(interval));
          final var suffix = IntervalAlgebra.intersect(this.getInterval(index), IntervalAlgebra.strictUpperBoundsOf(interval));

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
          final var suffix = IntervalAlgebra.intersect(this.getInterval(index), IntervalAlgebra.strictUpperBoundsOf(interval));

          this.segments.set(index, Segment.of(suffix, this.getValue(index)));
        }
      }

      // now, everything left of `index` is strictly left of `interval`,
      // and everything right of `index` is strictly right of `interval`,
      // so adding this interval to the list is trivial.
      this.segments.add(index, Segment.of(interval, value));

      return this;
    }

    public Builder<V> unset(Interval interval) {
      if (this.built) throw new IllegalStateException();

      if (interval.isEmpty()) return this;

      for (int i = 0; i < this.segments.size(); i++) {
        final var existingInterval = this.segments.get(i).interval();

        if (IntervalAlgebra.endsStrictlyBefore(existingInterval, interval)) continue;
        else if (IntervalAlgebra.startsStrictlyAfter(existingInterval, interval)) break;

        if (IntervalAlgebra.startsBefore(interval, existingInterval)) {
          final var segment = this.segments.remove(i);
          if (IntervalAlgebra.contains(interval, existingInterval)) {
            i--;
          } else {
            this.segments.add(Segment.of(Interval.between(interval.end, interval.endInclusivity.opposite(), existingInterval.end, existingInterval.endInclusivity), segment.value()));
          }
        } else {
          final var segment = this.segments.remove(i);
          this.segments.add(Segment.of(Interval.between(existingInterval.start, existingInterval.startInclusivity, interval.start, interval.startInclusivity.opposite()), segment.value()));
          if (IntervalAlgebra.endsAfter(existingInterval, interval)) {
            this.segments.add(Segment.of(Interval.between(interval.end, Interval.FOREVER.endInclusivity.opposite(), existingInterval.end, existingInterval.endInclusivity), segment.value()));
            i++;
          }
        }
      }

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
