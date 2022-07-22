package gov.nasa.jpl.aerie.constraints.time;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

final class IntervalMap<V> {
  private final IntervalAlgebra<Windows.WindowAlgebra, Window> alg;


  //[0, 3) [3] (3,5) -> would be 3 windows, one is zero width, we ARE allowing this.

  // INVARIANT: `intervals` is list of non-empty, non-overlapping intervals in ascending order.
  // INVARIANT: If two adjacent intervals abut exactly (e.g. [0, 3), [3, 5]), their values are non-equal.
  private final List<Pair<Window, V>> segments;

  private IntervalMap(final IntervalAlgebra<Windows.WindowAlgebra, Window> algebra, final List<Pair<Window, V>> segments) {
    this.alg = Objects.requireNonNull(algebra);
    this.segments = Objects.requireNonNull(segments);
  }

  // Empty constructor
  public IntervalMap(final IntervalAlgebra<Windows.WindowAlgebra, Window> algebra) {
    this(algebra, new ArrayList<>());
  }

  // Singleton constructor
  public IntervalMap(final IntervalAlgebra<Windows.WindowAlgebra, Window> algebra, final Window key, final V value) {
    this(algebra, new ArrayList<>(List.of(Pair.of(Objects.requireNonNull(key), Objects.requireNonNull(value)))));
  }

  // Copy constructor
  public IntervalMap(final IntervalMap<V> other) {
    this(other.alg, new ArrayList<>(other.segments));
  }

  public List<Pair<Window, V>> get(final Window interval) {
    final var results = new ArrayList<Pair<Window, V>>();
    for (final var segment : this.segments) {
      if (this.alg.overlaps(interval, segment.getKey())) {
        results.add(segment);
      }
    }

    return results;
  }

  public void set(final Window interval, final V value) {
    this.setAll(new IntervalMap<>(this.alg, interval, value));
  }

  public void setAll(final IntervalMap<V> other) {
    int index = 0;

    for (final var window : other.segments) {
      var interval = window.getKey();
      final var value = window.getValue();

      // <> is `interval`, the interval to unset; [] is the currently-indexed window in the map.
      // Cases: --[---]---<--->--
      while (index < this.segments.size() && this.alg.endsStrictlyBefore(this.getInterval(index), interval)) {
        index += 1;
      }

      // Cases: --[---<---]--->-- and --[---<--->---]--
      if (index < this.segments.size() && this.alg.startsBefore(this.getInterval(index), interval)) {
        // If the intervals agree on their value, we can unify the old interval with the new one.
        // Otherwise, we'll snip the old one.
        if (Objects.equals(this.getValue(index), value)) {
          interval = this.alg.unify(this.segments.remove(index).getKey(), interval);
        } else {
          final var prefix = this.alg.intersect(this.getInterval(index), this.alg.lowerBoundsOf(interval));
          final var suffix = this.alg.intersect(this.getInterval(index), this.alg.upperBoundsOf(interval));

          this.segments.set(index, Pair.of(prefix, this.getValue(index)));
          if (!this.alg.isEmpty(suffix)) this.segments.add(index + 1, Pair.of(suffix, this.getValue(index)));

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
          interval = this.alg.unify(this.segments.remove(index).getKey(), interval);
        } else {
          final var suffix = this.alg.intersect(this.getInterval(index), this.alg.upperBoundsOf(interval));

          this.segments.set(index, Pair.of(suffix, this.getValue(index)));
        }
      }

      // now, everything left of `index` is strictly left of `interval`,
      // and everything right of `index` is strictly right of `interval`,
      // so adding this interval to the list is trivial.
      this.segments.add(index, Pair.of(interval, value));
    }
  }

  public void unsetAll(final List<Pair<Window,V>> other) {
    for (var toUnset : other) {
      unset(toUnset.getKey());
    }
  }

  public void clear() {
    unsetAll(this.segments);
  }

  public void unset(final Window interval) {
    int index = 0;

    // <> is `interval`, the interval to unset; [] is the currently-indexed window in the map.
    // Cases: --[---]---<--->--
    while (index < this.segments.size() && this.alg.endsStrictlyBefore(this.getInterval(index), interval)) {
      index += 1;
    }

    // Cases: --[---<---]--->-- and --[---<--->---]--
    if (index < this.segments.size() && this.alg.startsBefore(this.getInterval(index), interval)) {
      // If the intervals agree on their value, we can unify the old interval with the new one.
      // Otherwise, we'll snip the old one.
      final var prefix = this.alg.intersect(this.getInterval(index), this.alg.lowerBoundsOf(interval));
      final var suffix = this.alg.intersect(this.getInterval(index), this.alg.upperBoundsOf(interval));

      this.segments.set(index, Pair.of(prefix, this.getValue(index)));
      if (!this.alg.isEmpty(suffix)) this.segments.add(index + 1, Pair.of(suffix, this.getValue(index)));

      index += 1;
    }

    // Cases: --<---[---]--->--
    while (index < this.segments.size() && !this.alg.endsAfter(this.getInterval(index), interval)) {
      this.segments.remove(index);
    }

    // Cases: --<---[--->---]--
    if (index < this.segments.size() && !this.alg.startsStrictlyAfter(this.getInterval(index), interval)) {
      final var suffix = this.alg.intersect(this.getInterval(index), this.alg.upperBoundsOf(interval));

      this.segments.set(index, Pair.of(suffix, this.getValue(index)));
    }

    // now, everything left of `index` is strictly left of `interval`,
    // and everything right of `index` is strictly right of `interval`,
    // so removing this interval from the list is trivial: do nothing!
  }

  /**
   * Authored by Jonathan
   * Maps intervals and the gaps between them in IntervalMap intervals to new values following some function transform
   *  which converts the old values (or nulls, in the case of gaps) to new values.
   *
   * @param intervals the IntervalMap whose values we wish to apply the transform over
   * @param transform the function to apply to each value of intervals (or the gaps between!), mapping its original value type V to another value type R
   * @return a new IntervalMap with newly mapped values
   * @param <V> The original value type of the IntervalMap that each interval corresponds to
   * @param <R> The new value type that the returned IntervalMap's intervals should correspond to
   */
  public static <V, R>
  IntervalMap<R> map(
      final IntervalMap<V> intervals,
      final Function<Optional<V>, Optional<R>> transform
  ) {
    final var alg = intervals.alg;
    final var segments = new ArrayList<Pair<Window, R>>();

    var previous = alg.bottom(); //in the context of Windows, a window at Duration.MIN; a minimum value when computing gaps at the next step
    for (final var segment : intervals.segments) {
      //previous might be ----TT---
      //segment might be  -------F-
      //gap is then       ------+--
      final var gap = alg.intersect(alg.upperBoundsOf(previous), alg.lowerBoundsOf(segment.getKey()));

      //we apply the transform to the gap (if it has contents)
      //currently we pass in an Optional.Empty to the function so it can handle a gap that isn't in our intervals, in case
      //  that should actually be mapped to a value (i.e. turn null into false for whatever reason). Then the return value
      //  of that, which is now Optional<R>, is checked for value, if it has any, add that and the new R value
      if (!alg.isEmpty(gap)) {
        transform.apply(Optional.empty()).ifPresent($ -> segments.add(Pair.of(gap, $)));
      }

      //apply the transform to the actual segment
      //returns an Optional<R>, check if it has value (it might not, if for example the transform maps Optional<V> where
      //  the value in that optional is true to null/Optional.empty(), in which case it won't have value and we don't
      //  wish to add to the map), and then if so add to the map
      transform.apply(Optional.of(segment.getValue())).ifPresent($ -> segments.add(Pair.of(segment.getKey(), $)));

      previous = segment.getKey();
    }

    {
      //check the final gap
      final var gap = alg.upperBoundsOf(previous);
      if (!alg.isEmpty(gap)) {
        transform.apply(Optional.empty()).ifPresent($ -> segments.add(Pair.of(gap, $)));
      }
    }

    return new IntervalMap<>(intervals.alg, segments);
  }

  //Jonathan's implementation
  public static <Alg, V1, V2, R>
  IntervalMap<R> map2(
      final IntervalMap<V1> left,
      final IntervalMap<V2> right,
      final BiFunction<Optional<V1>, Optional<V2>, Optional<R>> transform
  ) {
    final var alg = left.alg;

    final var accumulator = new Object() {
      final List<Pair<Window, R>> results = new ArrayList<>();
      Window accInterval = alg.bottom(); //left and right have the same alg - both use Windows. This was implemented before that was determined.
      Optional<R> accValue = Optional.empty(); //we are somehow accumulating a value into R

      void flush() {
        if (this.accValue.isPresent()) this.results.add(Pair.of(this.accInterval, this.accValue.get())); //if we have a value, add it
        this.accInterval = Window.atExclusive(this.accInterval.end); //reset interval TODO: better value??
        this.accValue = Optional.empty(); //reset acc value, probably doing this at the end of intervals or smth
      }

      void append(final Window interval, final Optional<R> newValue) {
        if (alg.isEmpty(interval)) return; //if nothing to append, don't act

        if (!alg.overlaps(this.accInterval, interval) || !Objects.equals(this.accValue, newValue)) { //if no longer an overlap or the values not equal, time to flush
          this.flush();
        }

        this.accInterval = alg.unify(this.accInterval, interval); //otherwise, merge the intervals if theres even a little overlap and the values r the same
        this.accValue = newValue; //update vlaue????
      }
    };

    int leftIndex = -1;
    Window leftInterval = alg.bottom();
    int rightIndex = -1;
    Window rightInterval = alg.bottom();
    System.out.println(leftInterval.toString() + " " +  rightInterval.toString());

    //handle -infinity up to the first interval
    {
      Window firstLeftPrefix;
      Window firstRightPrefix;
      if (left.segments.size() > 0) {
        //-infinity (or whatever the lowest point is) up to this point
        firstLeftPrefix = alg.lowerBoundsOf(left.getInterval(0));
      } else {
        //-infinity to infinity, because this is empty
        firstLeftPrefix = alg.upperBoundsOf(alg.bottom());
      }

      if (right.segments.size() > 0) {
        //-infinity (or whatever the lowest point is) up to this point
        firstRightPrefix = alg.lowerBoundsOf(right.getInterval(0));
      } else {
        //-infinity to infinity, because this is empty
        firstRightPrefix = alg.upperBoundsOf(alg.bottom());
      }

      final var overallPrefix = alg.intersect(firstLeftPrefix, firstRightPrefix);
      if (!alg.isEmpty(overallPrefix)) { //in the offchance that the first intervals start at Duration.MIN, it's worth checking
        accumulator.append(overallPrefix, transform.apply(
            Optional.empty(),
            Optional.empty()));
      }
    }

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
      final var leftPrefix = alg.intersect(leftInterval, alg.lowerBoundsOf(rightInterval));
      final var rightPrefix = alg.intersect(rightInterval, alg.lowerBoundsOf(leftInterval));
      final var overlap = alg.intersect(leftInterval, rightInterval);
      // At most one interval will have anything left over, but we need to analyze it against the next
      // interval on the opposing timeline.
      leftInterval = alg.intersect(leftInterval, alg.upperBoundsOf(alg.unify(leftPrefix, overlap)));
      rightInterval = alg.intersect(rightInterval, alg.upperBoundsOf(alg.unify(rightPrefix, overlap)));

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
      Window leftSuffix;
      Window rightSuffix;
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

  private Window getInterval(final int index) {
    final var i = (index >= 0) ? index : this.segments.size() - index;
    return this.segments.get(i).getKey();
  }

  private V getValue(final int index) {
    final var i = (index >= 0) ? index : this.segments.size() - index;
    return this.segments.get(i).getValue();
  }

  public Iterable<Pair<Window, V>> ascendingOrder() {
    // SAFETY: calling `.remove()` on the returned iterator does not breach encapsulation.
    // The same effect can be achieved by calling `windows.subtract()` against the data returned by the iterator,
    // except for the added burden of avoiding `ConcurrentModificationException`s.
    /*List<I> sortedIntervals = this.segments.stream().map($ -> $.getKey()).collect(Collectors.toList());
    sortedIntervals.sort(this.alg);
    List<Pair<I,V>> sortedIntervalsWithValues = new ArrayList<Pair<I,V>>();
    for (var interval : sortedIntervals) {
      sortedIntervalsWithValues.add(Pair.of(interval, get(interval)));
    }
    return sortedIntervalsWithValues;*/
    return this.segments;
  }

  public Iterable<Pair<Window,V>> descendingOrder() {
    return () -> new Iterator<>() {
      private final ListIterator<Pair<Window,V>> iter = IntervalMap.this.segments.listIterator();

      @Override
      public boolean hasNext() {
        return this.iter.hasPrevious();
      }
      @Override
      public Pair<Window,V> next() {
        return this.iter.previous();
      }
    };
  }

  public int size() {
    return segments.size();
  }

  public boolean isEmpty() {
    return segments.isEmpty();
  }

  public boolean includes(final Window interval, final V value) {
    return segments.contains(Pair.of(interval, value));
  }

  public static <V, R>
  R foldLeft(
      R initial,
      final IntervalMap<V> intervals,
      final BiFunction<V, R, R> transform
  ) {
    for (var interval : intervals.segments) {
      initial = transform.apply(interval.getValue(), initial);
    }
    return initial;
  }
}
