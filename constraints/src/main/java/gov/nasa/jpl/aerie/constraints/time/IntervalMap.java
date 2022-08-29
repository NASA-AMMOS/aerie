package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;

public final class IntervalMap<V> implements Iterable<Segment<V>> {

  // INVARIANT: `intervals` is list of non-empty, non-overlapping intervals in ascending order.
  // INVARIANT: If two adjacent intervals abut exactly (e.g. [0, 3), [3, 5]), their values are non-equal.
  private final List<Segment<V>> segments;

  public IntervalMap() {
    this.segments = new ArrayList<>();
  }

  public IntervalMap(final List<Segment<V>> segments) {
    this.segments = new ArrayList<>(segments.size());
    for (final var segment: segments) {
      this.setInternal(segment.interval(), segment.value(), 0);
    }
  }

  public IntervalMap(final Interval interval, final V value) {
    this.segments = List.of(Segment.of(interval, value));
  }

  @SafeVarargs
  public IntervalMap(final Segment<V>... segments) {
    this();
    for (final var segment: segments) {
      this.setInternal(segment.interval(), segment.value(), 0);
    }
  }

  public IntervalMap(final List<Interval> intervals, final V value) {
    this(
        intervals.stream().map($ -> Segment.of($, value)).toList()
    );
  }

  // Copy constructor
  public IntervalMap(final IntervalMap<V> other) {
    this(new ArrayList<>(other.segments));
  }

  public IntervalMap(final V value) {
   this(Interval.FOREVER, value);
  }

  public IntervalMap<V> set(final Interval interval, final V value) {
    return set(new IntervalMap<>(interval, value));
  }

  public IntervalMap<V> set(final List<Interval> intervals, final V value) {
    return set(new IntervalMap<>(intervals, value));
  }

  public IntervalMap<V> set(final IntervalMap<V> other) {
    final var result = new IntervalMap<>(this);
    int index = 0;
    for (final var segment: other) {
      index = result.setInternal(segment.interval(), segment.value(), index);
    }
    return result;
  }

  int setInternal(Interval interval, final V value, int index) {
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

    return index;
  }

  public IntervalMap<V> unset(final Interval interval) {
    return unset(new IntervalMap<>(interval, new Object()));
  }

  public IntervalMap<V> unset(final List<Interval> intervals) {
    return unset(new IntervalMap<>(intervals, new Object()));
  }

  public IntervalMap<V> unset(final IntervalMap<?> other) {
    return map2(
        this, other,
        (l, r) -> r.isPresent() ? Optional.empty() : l
    );
  }

  public IntervalMap<V> select(final Interval bounds) {
    return unset(List.of(
        interval(Duration.MIN_VALUE, Inclusive, bounds.start, bounds.startInclusivity.opposite()),
        interval(bounds.end, bounds.endInclusivity.opposite(), Duration.MAX_VALUE, Inclusive)
    ));
  }

  public IntervalMap<V> select(final List<Interval> intervals) {
    return map2(
        this, new IntervalMap<>(intervals, new Object()),
        (l, r) -> r.isPresent() ? l : Optional.empty()
    );
  }

  /**
   * Authored by Jonathan
   * Maps intervals and the gaps between them in IntervalMap intervals to new values following some function transform
   *  which converts the old values (or nulls, in the case of gaps) to new values.
   *
   * @param transform the function to apply to each value of intervals (or the gaps between!), mapping its original value type V to another value type R
   * @return a new IntervalMap with newly mapped values
   * @param <R> The new value type that the returned IntervalMap's intervals should correspond to
   */
  public <R> IntervalMap<R> map(final Function<Optional<V>, Optional<R>> transform) {
    return this.contextMap((v, i) -> transform.apply(v));
  }

  public <R> IntervalMap<R> contextMap(final BiFunction<Optional<V>, Interval, Optional<R>> transform) {
    final var segments = new ArrayList<Segment<R>>();

    var previous = Interval.at(Duration.MIN_VALUE); //in the context of Windows, a interval at Duration.MIN; a minimum value when computing gaps at the next step
    R previousValue = null;
    for (final var segment : this.segments) {
      //previous might be ----TT---
      //segment might be  -------F-
      //gap is then       ------+--
      final var gap = IntervalAlgebra.intersect(IntervalAlgebra.upperBoundsOf(previous), IntervalAlgebra.lowerBoundsOf(segment.interval()));

      //we apply the transform to the gap (if it has contents)
      //currently we pass in an Optional.Empty to the function so it can handle a gap that isn't in our intervals, in case
      //  that should actually be mapped to a value (i.e. turn null into false for whatever reason). Then the return value
      //  of that, which is now Optional<R>, is checked for value, if it has any, add that and the new R value
      if (!IntervalAlgebra.isEmpty(gap)) {
        final var result = transform.apply(Optional.empty(), gap);
        previousValue = insertAndCoalesce(segments, previousValue, gap, result);
      }

      //apply the transform to the actual segment
      //returns an Optional<R>, check if it has value (it might not, if for example the transform maps Optional<V> where
      //  the value in that optional is true to null/Optional.empty(), in which case it won't have value and we don't
      //  wish to add to the map), and then if so add to the map
      final var result = transform.apply(Optional.of(segment.value()), segment.interval());
      previousValue = insertAndCoalesce(segments, previousValue, segment.interval(), result);

      previous = segment.interval();
    }

    //check the final gap
    final var gap = IntervalAlgebra.upperBoundsOf(previous);
    if (!IntervalAlgebra.isEmpty(gap)) {
      final var result = transform.apply(Optional.empty(), gap);
      insertAndCoalesce(segments, previousValue, gap, result);
    }

    return new IntervalMap<>(segments);
  }

  private static <R> R insertAndCoalesce(
      final ArrayList<Segment<R>> segments,
      final R previousValue,
      final Interval interval,
      final Optional<R> result)
  {
    if (result.isPresent()) {
      final var value = result.get();
      if (previousValue != null && previousValue.equals(value)) {
        final var newInterval = Interval.unify(segments.get(segments.size()-1).interval(), interval);
        segments.add(Segment.of(
            newInterval,
            previousValue
        ));
        return previousValue;
      } else {
        segments.add(Segment.of(interval, value));
        return value;
      }
    } else {
      return null;
    }
  }

  //Jonathan's implementation
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

  /**
   * The left bound of the leftmost defined interval.
   * @return the leftmost bound and its inclusivity
   */
  public Optional<Pair<Duration, Interval.Inclusivity>> minValidTimePoint() {
    if(!this.isEmpty()) {
      final var window = this.segments.get(0).interval();
      return Optional.of(Pair.of(window.start, window.startInclusivity));
    } else{
      return Optional.empty();
    }
  }

  /**
   * The right bound of the rightmost defined interval.
   * @return the rightmost bound and its inclusivity
   */
  public Optional<Pair<Duration, Interval.Inclusivity>> maxValidTimePoint() {
    if(!isEmpty()) {
      final var window = this.segments.get(this.segments.size() - 1).interval();
      return Optional.of(Pair.of(window.end, window.endInclusivity));
    } else{
      return Optional.empty();
    }
  }

  private Interval getInterval(final int index) {
    final var i = (index >= 0) ? index : this.segments.size() - index;
    return this.segments.get(i).interval();
  }

  private V getValue(final int index) {
    final var i = (index >= 0) ? index : this.segments.size() - index;
    return this.segments.get(i).value();
  }

  public Segment<V> get(final int index) {
    final var i = (index >= 0) ? index : this.segments.size() - index;
    return this.segments.get(i);
  }

  public int size() {
    return segments.size();
  }

  public boolean isEmpty() {
    return segments.isEmpty();
  }

  @Override
  public Iterator<Segment<V>> iterator() {
    return this.segments.iterator();
  }

  public Iterable<Interval> iterateEqualTo(final V value) {
    return () -> segments
        .stream()
        .filter($ -> $.value().equals(value))
        .map(Segment::interval)
        .iterator();
  }

  public boolean isAllEqualTo(final V value) {
    for (final var segment: segments) {
      if (!segment.value().equals(value)) {
        return false;
      }
    }
    return true;
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
