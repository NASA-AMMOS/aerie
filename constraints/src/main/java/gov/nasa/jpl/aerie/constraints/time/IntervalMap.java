package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;

public final class IntervalMap<V> implements Iterable<Segment<V>> {

  private final IntervalAlgebra alg;


  //[0, 3) [3] (3,5) -> would be 3 windows, one is zero width, we ARE allowing this.

  // INVARIANT: `intervals` is list of non-empty, non-overlapping intervals in ascending order.
  // INVARIANT: If two adjacent intervals abut exactly (e.g. [0, 3), [3, 5]), their values are non-equal.
  private final List<Segment<V>> segments;

  public IntervalMap() {
    this.segments = new ArrayList<>();
    this.alg = new IntervalAlgebra(Interval.FOREVER);
  }

  public IntervalMap(final IntervalAlgebra algebra, final List<Segment<V>> segments) {
    this(algebra);
    for (final var segment: segments) {
      this.setInternal(segment.interval(), segment.value(), 0);
    }
  }

  public IntervalMap(final List<Segment<V>> segments) {
    this.alg = new IntervalAlgebra();
    this.segments = new ArrayList<>(segments.size());
    for (final var segment: segments) {
      this.setInternal(segment.interval(), segment.value(), 0);
    }
  }

  public IntervalMap(final IntervalAlgebra algebra) {
    this.alg = algebra;
    this.segments = new ArrayList<>();
  }

  public IntervalMap(final Interval interval, final V value) {
    this.alg = new IntervalAlgebra(Interval.FOREVER);
    this.segments = List.of(Segment.of(interval, value));
  }

  @SafeVarargs
  public IntervalMap(final Segment<V>... segments) {
    this();
    for (final var segment: segments) {
      this.setInternal(segment.interval(), segment.value(), 0);
    }
  }

  public IntervalMap(final IntervalAlgebra algebra, final Interval key, final V value) {
    this(algebra, new ArrayList<>(List.of(Segment.of(Objects.requireNonNull(key), Objects.requireNonNull(value)))));
  }

  public IntervalMap(final List<Interval> intervals, final V value) {
    this(
        new IntervalAlgebra(Interval.FOREVER),
        intervals.stream().map($ -> Segment.of($, value)).toList()
    );
  }

  // Copy constructor
  public IntervalMap(final IntervalMap<V> other) {
    this(other.alg, new ArrayList<>(other.segments));
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
    interval = Interval.intersect(this.alg.bounds(), interval);
    if (interval.isEmpty()) return index;

    //truncate interval to the bottom
    if (interval.start.shorterThan(this.alg.bottom().end)) {
      interval = interval(
          this.alg.bottom().end,
          Inclusive,
          interval.end,
          interval.endInclusivity
      );
    }

    // <> is `interval`, the interval to unset; [] is the currently-indexed interval in the map.
    // Cases: --[---]---<--->--
    while (index < this.segments.size() && this.alg.endsStrictlyBefore(this.getInterval(index), interval)) {
      index += 1;
    }

    // Cases: --[---<---]--->-- and --[---<--->---]--
    if (index < this.segments.size() && this.alg.startsBefore(this.getInterval(index), interval)) {
      // If the intervals agree on their value, we can unify the old interval with the new one.
      // Otherwise, we'll snip the old one.
      if (Objects.equals(this.getValue(index), value)) {
        interval = this.alg.unify(this.segments.remove(index).interval(), interval);
      } else {
        final var prefix = this.alg.intersect(this.getInterval(index), this.alg.lowerBoundsOf(interval));
        final var suffix = this.alg.intersect(this.getInterval(index), this.alg.upperBoundsOf(interval));

        this.segments.set(index, Segment.of(prefix, this.getValue(index)));
        if (!this.alg.isEmpty(suffix)) this.segments.add(index + 1, Segment.of(suffix, this.getValue(index)));

        index += 1;
      }
    }

    // Cases: --<---[---]--->--
    while (index < this.segments.size() && !this.alg.endsAfter(this.getInterval(index), interval)) {
      this.segments.remove(index);
    }

    // Cases: --<---[--->---]--
    if (index < this.segments.size() && !this.alg.startsStrictlyAfter(this.getInterval(index), interval)) {
      // If the intervals agree on their value, we can unify the old interval with the new one.
      // Otherwise, we'll snip the old one.
      if (Objects.equals(this.getValue(index), value)) {
        interval = this.alg.unify(this.segments.remove(index).interval(), interval);
      } else {
        final var suffix = this.alg.intersect(this.getInterval(index), this.alg.upperBoundsOf(interval));

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

    var previous = alg.bottom(); //in the context of Windows, a interval at Duration.MIN; a minimum value when computing gaps at the next step
    R previousValue = null;
    for (final var segment : this.segments) {
      //previous might be ----TT---
      //segment might be  -------F-
      //gap is then       ------+--
      final var gap = alg.intersect(alg.upperBoundsOf(previous), alg.lowerBoundsOf(segment.interval()));

      //we apply the transform to the gap (if it has contents)
      //currently we pass in an Optional.Empty to the function so it can handle a gap that isn't in our intervals, in case
      //  that should actually be mapped to a value (i.e. turn null into false for whatever reason). Then the return value
      //  of that, which is now Optional<R>, is checked for value, if it has any, add that and the new R value
      if (!alg.isEmpty(gap)) {
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
    final var gap = alg.upperBoundsOf(previous);
    if (!alg.isEmpty(gap)) {
      final var result = transform.apply(Optional.empty(), gap);
      insertAndCoalesce(segments, previousValue, gap, result);
    }

    return new IntervalMap<>(this.alg, segments);
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
    final var alg = left.alg;

    final var accumulator = new Object() {
      final ArrayList<Segment<R>> results = new ArrayList<>();
      Interval accInterval = alg.bottom();
      Optional<R> accValue = Optional.empty();

      void flush() {
        //if we have a value, add it
        this.accValue.ifPresent(r -> this.results.add(Segment.of(this.accInterval, r)));
        this.accInterval = Interval.atExclusive(this.accInterval.end); //reset interval TODO: better value??
        this.accValue = Optional.empty(); //reset acc value, probably doing this at the end of intervals or smth
      }

      void append(final Interval interval, final Optional<R> newValue) {
        if (interval.isEmpty()) return; //if nothing to append, don't act

        if (!(alg.overlaps(this.accInterval, interval) || alg.meets(this.accInterval, interval)) || !Objects.equals(this.accValue, newValue)) { //if no longer an overlap or the values not equal, time to flush
          this.flush();
        }

        this.accInterval = alg.unify(this.accInterval, interval); //otherwise, merge the intervals if theres even a little overlap and the values r the same
        this.accValue = newValue;
      }
    };

    int leftIndex = -1;
    Interval leftInterval = alg.bottom();
    int rightIndex = -1;
    Interval rightInterval = alg.bottom();

    //handle everything between
    while (leftIndex < left.segments.size() || rightIndex < right.segments.size()) {
      // Advance to the next interval, if we've fully consumed the one prior.
      if (alg.isEmpty(leftInterval)) { //i.e. if we are at the edge of the interval
        leftIndex++; //it's weird to increment and check again like this. but we need to increment only in the isEmpty
                     // case. after that, if we have just incremented, and we are in bounds, we want to check if we are
                     // in bounds. Not doing this can cause an  infinite loop!
        if (leftIndex < left.segments.size()) {
          leftInterval = left.getInterval(leftIndex);
        }
      }
      if (alg.isEmpty(rightInterval)) {
        rightIndex++;
        if (rightIndex < right.segments.size()) {
          rightInterval = right.getInterval(rightIndex);
        }
      }

      // Extract the prefix and overlap between these intervals.
      // At most one prefix will be non-empty (they can't each start before the other),
      // but pretending otherwise makes the code simpler.
      final var nullGap = alg.intersect(alg.intersect(alg.lowerBoundsOf(rightInterval), alg.lowerBoundsOf(leftInterval)),
                                          alg.upperBoundsOf(accumulator.accInterval));
      final var leftPrefix = alg.intersect(leftInterval, alg.lowerBoundsOf(rightInterval));
      final var rightPrefix = alg.intersect(rightInterval, alg.lowerBoundsOf(leftInterval));
      final var overlap = alg.intersect(leftInterval, rightInterval);
      // At most one interval will have anything left over, but we need to analyze it against the next
      // interval on the opposing timeline.
      leftInterval = alg.intersect(leftInterval, alg.upperBoundsOf(alg.unify(leftPrefix, overlap)));
      rightInterval = alg.intersect(rightInterval, alg.upperBoundsOf(alg.unify(rightPrefix, overlap)));

      if (!alg.isEmpty(nullGap)) {
        accumulator.append(nullGap, transform.apply(
            Optional.empty(),
            Optional.empty()
        ));
      }
      // Compute a new value on each interval.
      if (!alg.isEmpty(leftPrefix)) {
        accumulator.append(leftPrefix, transform.apply(
            Optional.of(left.getValue(leftIndex)),
            Optional.empty()));
      }
      if (!alg.isEmpty(rightPrefix)) {
        accumulator.append(rightPrefix, transform.apply(
            Optional.empty(),
            Optional.of(right.getValue(rightIndex))));
      }
      if (!alg.isEmpty(overlap)) {
        accumulator.append(overlap, transform.apply(
            Optional.of(left.getValue(leftIndex)),
            Optional.of(right.getValue(rightIndex))));
      }
    }

    //handle end -> infinity, but only if we haven't earlier (which happens if both left and right are empty)
    if (left.segments.size() > 0 || right.segments.size() > 0)
    {
      Interval leftSuffix;
      Interval rightSuffix;
      if (left.segments.size() > 0) {
        //-infinity (or whatever the lowest point is) up to this point
        leftSuffix = alg.upperBoundsOf(left.getInterval(left.segments.size()-1));
      } else {
        //-infinity to infinity, because this is empty
        leftSuffix = alg.upperBoundsOf(alg.bottom());
      }

      if (right.segments.size() > 0) {
        //-infinity (or whatever the lowest point is) up to this point
        rightSuffix = alg.upperBoundsOf(right.getInterval(right.segments.size()-1));
      } else {
        //-infinity to infinity, because this is empty
        rightSuffix = alg.upperBoundsOf(alg.bottom());
      }

      final var overallPrefix = alg.intersect(leftSuffix, rightSuffix);
      if (!alg.isEmpty(overallPrefix)) { //in the offchance that the first intervals start at Duration.MIN, it's worth checking
        accumulator.append(overallPrefix, transform.apply(
            Optional.empty(),
            Optional.empty()));
      }
    }

    accumulator.flush(); //flush the final interval

    return new IntervalMap<>(alg, accumulator.results);
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
    return () -> segments.stream().flatMap(pair -> {
      if (pair.value().equals(value)) {
        return Stream.of(pair.interval());
      } else {
        return Stream.of();
      }
    }).iterator();
  }

  public Spliterator<Interval> spliterateEqualTo(final V value) {
    return StreamSupport.stream(this.spliterator(), false).flatMap(pair -> {
      if (pair.value().equals(value)) {
        return Stream.of(pair.interval());
      } else {
        return Stream.of();
      }
    }).spliterator();
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

  public IntervalAlgebra getAlg() {
    return alg;
  }

  @Override
  public String toString() {
    return this.segments.toString();
  }
}
