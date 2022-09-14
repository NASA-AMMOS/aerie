package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;

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

  // INVARIANT: `intervals` is list of non-empty, non-overlapping intervals in ascending order.
  // INVARIANT: If two adjacent intervals abut exactly (e.g. [0, 3), [3, 5]), their values are non-equal.
  private final List<Segment<V>> segments;

  /** Create an empty IntervalMap. */
  public IntervalMap() {
    this.segments = new ArrayList<>();
  }

  /**
   * Creates an IntervalMap from a potentially un-ordered or overlapping list of segments.
   *
   * Segments are ordered. In the case of overlapping segments, the latter value in the list
   * overwrites the former.
   */
  public IntervalMap(final List<Segment<V>> segments) {
    this.segments = new ArrayList<>(segments.size());
    for (final var segment: segments) {
      this.insertInPlace(segment, 0);
    }
  }

  /** Creates an IntervalMap with a single segment. */
  public IntervalMap(final Interval interval, final V value) {
    this.segments = List.of(Segment.of(interval, value));
  }

  /**
   * Creates an IntervalMap from a potentially un-ordered or overlapping list of segments.
   *
   * Delegates to {@link IntervalMap#IntervalMap(List)}.
   */
  @SafeVarargs
  public IntervalMap(final Segment<V>... segments) {
    this();
    for (final var segment: segments) {
      this.insertInPlace(segment, 0);
    }
  }

  /**
   * Creates an IntervalMap with many intervals all associated with the same value.
   *
   * Delegates to {@link IntervalMap#IntervalMap(List)}.
   */
  public IntervalMap(final List<Interval> intervals, final V value) {
    this(
        intervals.stream().map($ -> Segment.of($, value)).toList()
    );
  }

  /** Creates an IntervalMap that is equal to a single value for all representable time */
  public IntervalMap(final V value) {
    this(Interval.FOREVER, value);
  }

  /**
   * Inserts, overwrites, and coalesces a new segment into the map.
   *
   * This is a package-private helper function, only intended for use by IntervalMap
   * and profile specializations like {@link Windows}. It does mutate `this`.
   *
   * @param segment segment to insert
   * @param index index of the earliest segment in the map to check for overwriting and coalescing.
   *              Used to optimize the insertion of consecutive segments.
   *              If unsure about what value it should be, provide `0`.
   * @return a new index, which will be valid input to the `index` argument if `insertInPlace` is called
   *         again with another segment which is strictly after the current `segment`.
   */
  int insertInPlace(final Segment<V> segment, int index) {
    Interval interval = segment.interval();
    final V value = segment.value();

    if (interval.isEmpty()) return index;

    // <> is `interval`, the interval to unset; [] is the currently-indexed interval in the map.
    // Cases: --[---]---<--->--
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

    return index;
  }

  /**
   * Sets an interval to a value.
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> set(final Interval interval, final V value) {
    return set(new IntervalMap<>(interval, value));
  }

  /**
   * Sets a list of intervals to a single value.
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> set(final List<Interval> intervals, final V value) {
    return set(new IntervalMap<>(intervals, value));
  }

  /**
   * Sets all segments from another IntervalMap on top of this map's segments.
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> set(final IntervalMap<V> other) {
    return map2(this, other, (a, b) -> (b.isPresent()) ? b : a);
  }

  /**
   * Unsets the given intervals.
   *
   * Turns the given intervals into gaps, if they are not already.
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> unset(final Interval... intervals) {
    return unset(Arrays.stream(intervals).toList());
  }

  /**
   * Unsets the given intervals.
   *
   * Turns the given intervals into gaps, if they are not already.
   *
   * @return a new IntervalMap
   */
  public IntervalMap<V> unset(final List<Interval> intervals) {
    return map2(this, new IntervalMap<>(intervals, new Object()), (l, r) -> r.isEmpty() ? l : Optional.empty());
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
    return map2(
        this, new IntervalMap<>(intervals, new Object()),
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
  public <R> IntervalMap<R> map(final Function<Optional<V>, Optional<R>> transform) {
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
  public <R> IntervalMap<R> map(final BiFunction<Optional<V>, Interval, Optional<R>> transform) {
    final var result = new IntervalMap<R>();
    var resultIndex = 0;

    var previous =
        Interval.at(Duration.MIN_VALUE); //in the context of Windows, a interval at Duration.MIN; a minimum value when computing gaps at the next step
    for (final var segment : this.segments) {
      //previous might be ----TT---
      //segment might be  -------F-
      //gap is then       ------+--
      final var gap = IntervalAlgebra.intersect(
          IntervalAlgebra.strictUpperBoundsOf(previous),
          IntervalAlgebra.strictLowerBoundsOf(segment.interval()));

      //we apply the transform to the gap (if it has contents)
      //currently we pass in an Optional.Empty to the function so it can handle a gap that isn't in our intervals, in case
      //  that should actually be mapped to a value (i.e. turn null into false for whatever reason). Then the return value
      //  of that, which is now Optional<R>, is checked for value, if it has any, add that and the new R value
      if (!IntervalAlgebra.isEmpty(gap)) {
        final var value = transform.apply(Optional.empty(), gap);
        if (value.isPresent())
          resultIndex = result.insertInPlace(Segment.of(gap, value.get()), resultIndex);
      }

      //apply the transform to the actual segment
      //returns an Optional<R>, check if it has value (it might not, if for example the transform maps Optional<V> where
      //  the value in that optional is true to null/Optional.empty(), in which case it won't have value and we don't
      //  wish to add to the map), and then if so add to the map
      final var value = transform.apply(Optional.of(segment.value()), segment.interval());
      if (value.isPresent())
        resultIndex = result.insertInPlace(Segment.of(segment.interval(), value.get()), resultIndex);

      previous = segment.interval();
    }

    //check the final gap
    final var gap = IntervalAlgebra.strictUpperBoundsOf(previous);
    if (!IntervalAlgebra.isEmpty(gap)) {
      final var value = transform.apply(Optional.empty(), gap);
      if (value.isPresent())
        resultIndex = result.insertInPlace(Segment.of(gap, value.get()), resultIndex);
    }

    return result;
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
    final var resultMap = new IntervalMap<R>();
    final List<Segment<R>> result = resultMap.segments;

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

    return resultMap;
  }

  /** Gets the interval at a given segment index */
  private Interval getInterval(final int index) {
    final var i = (index >= 0) ? index : this.segments.size() - index;
    return this.segments.get(i).interval();
  }

  /** Gets the value at a given segment index */
  private V getValue(final int index) {
    final var i = (index >= 0) ? index : this.segments.size() - index;
    return this.segments.get(i).value();
  }

  /** Gets the segment at a given index */
  public Segment<V> get(final int index) {
    final var i = (index >= 0) ? index : this.segments.size() - index;
    return this.segments.get(i);
  }

  /** The number of defined intervals in this. */
  public int size() {
    return segments.size();
  }

  /** Whether this has no defined segments */
  public boolean isEmpty() {
    return segments.isEmpty();
  }

  @Override
  public Iterator<Segment<V>> iterator() {
    return this.segments.iterator();
  }

  /** Creates an iterable over the Intervals where this map is equal to a value */
  public Iterable<Interval> iterateEqualTo(final V value) {
    return () -> segments
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
}
