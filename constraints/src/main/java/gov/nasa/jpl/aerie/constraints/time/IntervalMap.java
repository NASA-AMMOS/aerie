package gov.nasa.jpl.aerie.constraints.time;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

final class IntervalMap<Alg, I, V> {
  private final IntervalAlgebra<Alg, I> alg;


  //[0, 3) [3] (3,5) -> would be 3 windows, one is zero width, we ARE allowing this.

  // INVARIANT: `intervals` is list of non-empty, non-overlapping intervals in ascending order.
  // INVARIANT: If two adjacent intervals abut exactly (e.g. [0, 3), [3, 5]), their values are non-equal.
  private final List<Pair<I, V>> segments;

  private IntervalMap(final IntervalAlgebra<Alg, I> algebra, final List<Pair<I, V>> segments) {
    this.alg = Objects.requireNonNull(algebra);
    this.segments = Objects.requireNonNull(segments);
  }

  // Empty constructor
  public IntervalMap(final IntervalAlgebra<Alg, I> algebra) {
    this(algebra, new ArrayList<>());
  }

  // Singleton constructor
  public IntervalMap(final IntervalAlgebra<Alg, I> algebra, final I key, final V value) {
    this(algebra, new ArrayList<>(List.of(Pair.of(Objects.requireNonNull(key), Objects.requireNonNull(value)))));
  }

  // Copy constructor
  public IntervalMap(final IntervalMap<Alg, I, V> other) {
    this(other.alg, new ArrayList<>(other.segments));
  }

  public List<Pair<I, V>> get(final I interval) {
    final var results = new ArrayList<Pair<I, V>>();
    for (final var segment : this.segments) {
      if (this.alg.overlaps(interval, segment.getKey())) {
        results.add(segment);
      }
    }

    return results;
  }

  public void set(final I interval, final V value) {
    this.setAll(new IntervalMap<>(this.alg, interval, value));
  }

  public void setAll(final IntervalMap<Alg, I, V> other) {
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

  public void unsetAll(final List<Pair<I,V>> other) {
    for (var toUnset : other) {
      unset(toUnset.getKey());
    }
  }

  public void unset(final I interval) {
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
   * @param <Alg> The algebra used by the IntervalMap
   * @param <I> The type of intervals
   * @param <V> The original value type of the IntervalMap that each interval corresponds to
   * @param <R> The new value type that the returned IntervalMap's intervals should correspond to
   */
  public static <Alg, I, V, R>
  IntervalMap<Alg, I, R> map(
      final IntervalMap<Alg, I, V> intervals,
      final Function<Optional<V>, Optional<R>> transform
  ) {
    final var alg = intervals.alg;
    final var segments = new ArrayList<Pair<I, R>>();

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

  public static <Alg, I, V1, V2, R>
  IntervalMap<Alg, I, R> map2(
      final IntervalMap<Alg, I, V1> left,
      final IntervalMap<Alg, I, V2> right,
      final BiFunction<Optional<V1>, Optional<V2>, Optional<R>> transform
  ) {
    // TODO
    //throw new NotImplementedException();

    /*
      This works by shifting a pointer at the end of each intersection. For example, let's say we have the following,
        where + implies the interval is defined, - implies the interval is not, ... implies the space between this interval
        and alg.bottom or the highest upper bound when doing alg.getUpperBound:

      left:   ...+++-+++--++-...
      right:  ...-+-++-++--++...

      we want every gap/space where definedness changes, i.e. we want the subinterval where left is defined, right isn't,
        and then the one where both are, the one where only left is, the one where only right is, etc. so our splits
        (we split based on when definedness for either left or right changes) would be:

      left:   ...|+|+|+|-|+|+|+|-|-|+|+|-|...
      right:  ...|-|+|-|+|+|-|+|+|-|-|+|+|...

      the goal of this method is to extract each of these splits (it is pretty awkward due to the existing Algebra definition)
        and then pass the values of those splits (so if left has a value and right doesn't, pass Optional.of(leftValue) and
        Optional.empty() for the right absence of value) to the transform, and add it to segments

      to do so, we use 4 "pointers". The implementation is a bit obfuscated owing to the Algebra, but the idea is:
        - one 'current' pointer that references the start of the current interval on the left. so an actual one that exists
        - a 'previous' pointer that references where the left side last left off, after the previous split was calculated - the gaps between these help us determine intervals
        - the same for the right

      so, when the left and right previous pointers occur before the current pointers, that must mean there is a gap for both left and right:
               P  C
        left:  --+++...
        right: ---++...

      when a previous pointer occurs at the same time as a current pointer, there is no gap and we are now extracting an interval over an existing left or right segment:
               P P
               C C
      left:  --+++...
      right: ----+...

      This breaks down into four cases, discussed below.
     */

    if (left.alg.getClass() != right.alg.getClass()) {
      throw new IllegalArgumentException("Grammars of left and right must match!");
    }
    final var alg = left.alg;
    final var segments = new ArrayList<Pair<I, R>>();

    var previousLeft = left.alg.bottom(); //PL - i think this encounters problems if the first interval starts at bottom, or if the final one ends at top, i THINK
    var previousRight = right.alg.bottom(); //PR

    var leftIndex = 0; //need to update this whenever you move up an interval in the list for bounds checking
    var currentLeft = left.segments.get(0).getKey(); //CL; this will be sliced depending on how intervals line up (i.e. if this represents an interval and previousRight is at a gap)
    var currentLeftValue = left.segments.get(0).getValue(); //store the value, as we will splice intervals and break 1:1 correspondence between whats stored in currentLeft and the actual value

    var rightIndex = 0;
    var currentRight = right.segments.get(0).getKey(); //CR
    var currentRightValue = right.segments.get(0).getValue(); //store the value, as we will splice intervals and break 1:1 correspondence between whats stored in currentLeft and the actual value

    while (leftIndex < left.segments.size() && rightIndex < right.segments.size()) {
      /*
          Four cases:
            - left is undefined, right is undefined (i.e. PL != CL, PR != CR)
            - left is defined, right is defined (i.e. PL = CL, PR = CR)
            - left is defined, right is undefined (i.e. PL = CL, PR != CR)
            - left is undefined, right is defined (i.e. PL != CL, PR = CR)
       */

      final I gap;

      //case 1, left is undefined, right is undefined (i.e. PL != CL, PR != CR)
      if (alg.contains(alg.lowerBoundsOf(currentLeft), alg.lowerBoundsOf(previousLeft)) &&
          alg.contains(alg.lowerBoundsOf(currentRight), alg.lowerBoundsOf(previousRight))) {
        //overlapping gap is the undefined space where lowerBounds(currentLeft) match that of lowerBounds(currentRight)
        gap = alg.intersect(alg.lowerBoundsOf(currentLeft), alg.lowerBoundsOf(currentRight));
        transform.apply(Optional.empty(), Optional.empty()).ifPresent($ -> segments.add(Pair.of(gap, $))); //won't be empty, implied by the condition
      }

      //case 2, left is defined, right is defined (i.e. PL = CL, PR = CR)
      else if (alg.equals(alg.lowerBoundsOf(currentLeft), alg.lowerBoundsOf(previousLeft)) &&
          alg.equals(alg.lowerBoundsOf(currentRight), alg.lowerBoundsOf(previousRight))) {
        //overlapping gap is the space where both are defined, so just the intersection
        gap =  alg.intersect(currentLeft, currentRight);
        transform.apply(Optional.of(currentLeftValue), Optional.of(currentRightValue)).ifPresent($ -> segments.add(Pair.of(gap, $)));

        if(alg.equals(alg.upperBoundsOf(gap), alg.upperBoundsOf(currentLeft))) {
          //that means the intersection goes up to the end of left
          leftIndex += 1;
          if (leftIndex < left.segments.size()) {
            currentLeft = left.segments.get(leftIndex).getKey();
            currentLeftValue = left.segments.get(leftIndex).getValue();
          }
        }
        else {
          //splice the interval, move currentLeft up to where the end of the interval is, i.e. intersection of upper
          //  bounds of the gap and the interval itself (what's left of the interval after the gap)
          currentLeft = alg.intersect(alg.upperBoundsOf(gap), currentLeft);
        }

        if(alg.equals(alg.upperBoundsOf(gap), alg.upperBoundsOf(currentRight))) {
          //that means the intersection goes up to the end of left
          rightIndex += 1;
          if (rightIndex < right.segments.size()) {
            currentRight = right.segments.get(rightIndex).getKey();
            currentRightValue = right.segments.get(rightIndex).getValue();
          }
        }
        else {
          //splice the interval, move currentLeft up to where the end of the interval is, i.e. intersection of upper
          //  bounds of the gap and the interval itself (what's left of the interval after the gap)
          currentRight = alg.intersect(alg.upperBoundsOf(gap), currentRight);
        }

      }

      //case 3, left is defined, right is undefined (i.e. PL = CL, PR != CR)
      else if (alg.equals(alg.lowerBoundsOf(currentLeft), alg.lowerBoundsOf(previousLeft)) &&
               alg.contains(alg.lowerBoundsOf(currentRight), alg.lowerBoundsOf(previousRight))) {
        //overlapping gap is the space where both are defined, so just the intersection
        gap =  alg.intersect(currentLeft, alg.lowerBoundsOf(currentRight));
        transform.apply(Optional.of(currentLeftValue), Optional.empty()).ifPresent($ -> segments.add(Pair.of(gap, $)));

        if(alg.equals(alg.upperBoundsOf(gap), alg.upperBoundsOf(currentLeft))) {
          //that means the intersection goes up to the end of left
          leftIndex += 1;
          if (leftIndex < left.segments.size()) {
            currentLeft = left.segments.get(leftIndex).getKey();
            currentLeftValue = left.segments.get(leftIndex).getValue();
          }
        }
        else {
          //splice the interval, move currentLeft up to where the end of the interval is, i.e. intersection of upper
          //  bounds of the gap and the interval itself (what's left of the interval after the gap)
          currentLeft = alg.intersect(alg.upperBoundsOf(gap), currentLeft);
        }
      }

      //case 4, left is undefined, right is defined (i.e. PL != CL, PR = CR)
      else {
        //overlapping gap is the space where both are defined, so just the intersection
        gap =  alg.intersect(alg.lowerBoundsOf(currentLeft), currentRight);
        transform.apply(Optional.empty(), Optional.of(currentRightValue)).ifPresent($ -> segments.add(Pair.of(gap, $)));

        if(alg.equals(alg.upperBoundsOf(gap), alg.upperBoundsOf(currentRight))) {
          //that means the intersection goes up to the end of left
          rightIndex += 1;
          if (rightIndex < right.segments.size()) {
            currentRight = right.segments.get(rightIndex).getKey();
            currentRightValue = right.segments.get(rightIndex).getValue();
          }
        }
        else {
          //splice the interval, move currentLeft up to where the end of the interval is, i.e. intersection of upper
          //  bounds of the gap and the interval itself (what's left of the interval after the gap)
          currentRight = alg.intersect(alg.upperBoundsOf(gap), currentRight);
        }
      }

      previousLeft = alg.upperBoundsOf(gap); //effectively points to the end of the gap
      previousRight = alg.upperBoundsOf(gap);

    }

    while (leftIndex < left.segments.size())
    {
      //check the final left intervals; case 3, left is defined, right is undefined (i.e. PL = CL, PR != CR)
      //overlapping gap is the space where both are defined, so just the intersection
      final var gap =  alg.intersect(currentLeft, alg.lowerBoundsOf(currentRight));
      transform.apply(Optional.of(currentLeftValue), Optional.empty()).ifPresent($ -> segments.add(Pair.of(gap, $)));

      if(alg.equals(alg.upperBoundsOf(gap), alg.upperBoundsOf(currentLeft))) {
        //that means the intersection goes up to the end of left
        leftIndex += 1;
        if (leftIndex < left.segments.size()) {
          currentLeft = left.segments.get(leftIndex).getKey();
          currentLeftValue = left.segments.get(leftIndex).getValue();
        }
      }
      else {
        //splice the interval, move currentLeft up to where the end of the interval is, i.e. intersection of upper
        //  bounds of the gap and the interval itself (what's left of the interval after the gap)
        currentLeft = alg.intersect(alg.upperBoundsOf(gap), currentLeft);
      }
    }


    while (rightIndex < left.segments.size())
    {
      //or check the final right intervals; case 4, left is undefined, right is defined (i.e. PL != CL, PR = CR)
      //overlapping gap is the space where both are defined, so just the intersection
      final var gap =  alg.intersect(alg.lowerBoundsOf(currentLeft), currentRight);
      transform.apply(Optional.empty(), Optional.of(currentRightValue)).ifPresent($ -> segments.add(Pair.of(gap, $)));

      if(alg.equals(alg.upperBoundsOf(gap), alg.upperBoundsOf(currentRight))) {
        //that means the intersection goes up to the end of left
        rightIndex += 1;
        if (rightIndex < right.segments.size()) {
          currentRight = right.segments.get(rightIndex).getKey();
          currentRightValue = right.segments.get(rightIndex).getValue();
        }
      }
      else {
        //splice the interval, move currentLeft up to where the end of the interval is, i.e. intersection of upper
        //  bounds of the gap and the interval itself (what's left of the interval after the gap)
        currentRight = alg.intersect(alg.upperBoundsOf(gap), currentRight);
      }
    }

    //check the final gap
    //overlapping gap is the undefined space where lowerBounds(currentLeft) match that of lowerBounds(currentRight)
    final var gap =  alg.intersect(alg.upperBoundsOf(currentLeft), alg.upperBoundsOf(currentRight));
    transform.apply(Optional.empty(), Optional.empty()).ifPresent($ -> segments.add(Pair.of(gap, $)));


    return new IntervalMap<>(alg, segments);
  }

  private I getInterval(final int index) {
    final var i = (index > 0) ? index : this.segments.size() - index;
    return this.segments.get(i).getKey();
  }

  private V getValue(final int index) {
    final var i = (index > 0) ? index : this.segments.size() - index;
    return this.segments.get(i).getValue();
  }

  public Iterable<Pair<I, V>> ascendingOrder() {
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

  public Iterable<Pair<I,V>> descendingOrder() {
    return () -> new Iterator<>() {
      private final ListIterator<Pair<I,V>> iter = IntervalMap.this.segments.listIterator();

      @Override
      public boolean hasNext() {
        return this.iter.hasPrevious();
      }
      @Override
      public Pair<I,V> next() {
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

  public boolean includes(final I interval, final V value) {
    return segments.contains(Pair.of(interval, value));
  }

  public static <Alg, I, V, R>
  R foldLeft(
      R initial,
      final IntervalMap<Alg, I, V> intervals,
      final BiFunction<V, R, R> transform
  ) {
    for (var interval : intervals.segments) {
      initial = transform.apply(interval.getValue(), initial);
    }
    return initial;
  }
}
