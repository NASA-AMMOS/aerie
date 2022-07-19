package gov.nasa.jpl.aerie.constraints.time;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class IntervalMap<Alg, I, V> {
  private final IntervalAlgebra<Alg, I> alg;

  // INVARIANT: `intervals` is a map of non-empty, non-overlapping intervals mapping to some value.
  private final Map<I, V> intervals = new HashMap();

  public IntervalMap(final IntervalAlgebra<Alg, I> algebra) {
    this.alg = Objects.requireNonNull(algebra);
  }

  public IntervalMap(final IntervalMap<Alg, I, V> other) {
    this.alg = other.alg;
    this.intervals.putAll(other.intervals);
  }


  public void add(final I interval, final V value) {
    if (this.alg.isEmpty(interval)) return;


    //List<I> intervalList = new ArrayList<>(intervals.keySet());
    //intervalList.sort(alg);
    // OPTIMIZATION: If this interval fits at the end of our list, just do that.
    // Common case for building up a set of intervals.
    // This whole clause can be removed without affecting correctness.
    /*if (
        intervalList.size() == 0 ||
        this.alg.endsStrictlyBefore(intervalList.get(intervalList.size() - 1), interval)
    ) {
      this.intervals.put(interval, value);
      return;
    } else if (!this.alg.startsBefore(interval, intervalList.get(intervalList.size() - 1))) {
      this.intervals.set(
          this.intervals.size() - 1,
          this.alg.unify(interval, this.intervals.get(this.intervals.size() - 1)));
      return;
    }

    this.addAll(List.of(interval));*

    //check if it overlaps with anything, if so, split intervals and replace with newest value. i.e.
    //If this is original:     [--TTTTTTT----]
    //and this is the new one: [-----FFFFF---]
    //result is:               [--TTTFFFFF---] (2 windows)
    //four cases:
    //  [--TTTTTTTT----]
    //  [----FFFFFFF---]
    //= [--TTFFFFFFF---] (right overlap)
    //  [--TTT---------]
    //  [FFF-----------]
    //= [FFFTT---------] (left overlap)
    //  [--TTTTTTTTT---]
    //  [-----FFF------]
    //= [--TTTFFFTTT---] (total old overlap)
    //  [-----TTT------]
    //  [---FFFFFF-----]
    //= [---FFFFFF-----] (total new overlap)
    //note - the actual values don't affect this operation as the map is agnostic to actual value types!
    for (I existingInterval : intervalList) {
      if (this.alg.overlaps(existingInterval, interval)) {
        I intersection = this.alg.intersect(existingInterval, interval);

        if (this.alg.equals(existingInterval, interval)) {
          this.intervals.remove(existingInterval);
          this.intervals.put(interval, value);
        }

        //total old overlap
        if (this.alg.strictlyContains(existingInterval, interval)) {

        }

        //total new overlap
        if (this.alg.contains(interval, existingInterval)) {
          this.intervals.remove(existingInterval);
          this.intervals.put(interval, value);
        }



        //right overlap
        if(this.alg.startsBefore(existingInterval, interval)) {


        }

        //left overlap
        if(this.alg.startsBefore(interval, existingInterval)) {

        }

      }
    }*/

    this.addAll(List.of(Pair.of(interval, value)));
  }

  public void addAll(final IntervalMap<Alg, I, V> other) {
    List<Pair<I,V>> pairs = new ArrayList<Pair<I,V>>();
    for (I key : other.intervals.keySet()) {
      pairs.add(Pair.of(key, other.intervals.get(key)));
    }
    this.addAll(pairs);
  }

  /**
   * PRECONDITION: `other` produces non-empty, non-overlapping intervals in ascending order.
   */
  private void addAll(final Iterable<Pair<I,V>> other) {
    List<I> intervalList = new ArrayList<>(intervals.keySet());
    intervalList.sort(alg);

    int index = 0;

    for (final var value : other) {
      var interval = value.getKey();
      // Skip windows that end before this one starts.
      while (
          index < intervalList.size() &&
          this.alg.endsStrictlyBefore(intervalList.get(index), interval)
      ) {
        index += 1;
      }

      // Remove any intervals that overlap this interval, place the interval after doing this pruning
      var joined = interval;
      while (
          index < intervalList.size() &&
          !this.alg.endsStrictlyBefore(interval, intervalList.get(index))
      ) {
        //now we have that the new interval and the existing interval overlap somehow. there are five ways in which they overlap:
        var current = intervalList.get(index);

        //equality - old: [---TTTTTT---]
        //           new: [---FFFFFF---]
        //strict new overlap - old: [-----TT-----]
        //                     new: [---FFFFF----]
        if (this.alg.equals(current, interval) || this.alg.contains(interval, current)) {
          this.intervals.remove(current);
        }

        //strict old overlap - old: [--TTTTTTT---]
        //                     new: [---FFF------]
        //                  result: [--TFFFTTT---]
        else if (this.alg.contains(current, interval)) {
          var currentValue = this.intervals.get(current);
          var before = this.alg.intersect(this.alg.lowerBoundsOf(interval), current);
          var after = this.alg.intersect(this.alg.upperBoundsOf(interval), current);
          this.intervals.remove(current);
          this.intervals.put(before, currentValue);
          this.intervals.put(after, currentValue);
        }

        //left overlap - old: [--FFFFF------]
        //               new: [----TTTT-----]
        else if (this.alg.endsBefore(current, interval)) {
          var currentValue = this.intervals.get(current);
          var before = this.alg.intersect(this.alg.lowerBoundsOf(interval), current);
          this.intervals.remove(current);
          this.intervals.put(before, currentValue);
        }

        //right overlap - old: [-----FFFF---]
        //                new: [---TTTT-----]
        else  {
          var currentValue = this.intervals.get(current);
          var after = this.alg.intersect(this.alg.upperBoundsOf(interval), current);
          this.intervals.remove(current);
          this.intervals.put(after, currentValue);
        }
      }
      this.intervals.put(interval, value.getValue());
    }
  }


  public void subtract(final I interval) {
    if (this.alg.isEmpty(interval)) return;
    this.subtractAll(List.of(interval));
  }

  public void subtractAll(final IntervalMap<Alg, I, V> other) {
    this.subtractAll(other.intervals.keySet());
  }

  /**
   * PRECONDITION: `other` produces non-empty, non-overlapping intervals in ascending order.
   */
  private void subtractAll(final Iterable<I> other) {
    List<I> intervalList = new ArrayList<>(intervals.keySet());
    intervalList.sort(alg);

    int index = 0;
    // We'll notate each `window` by <> and `this.intervals.get(index)` by [].
    for (final var window : other) {
      // Look for the first window ending at or after this one starts.
      // Skip these cases: --[---]---<--->--
      while (index < this.intervals.size() && this.alg.endsStrictlyBefore(this.intervals.get(index), window)) {
        index += 1;
      }

      // Clip the window at the start of this range.
      // Handle these cases: --[---<---]--->-- and --[---<--->---]--
      // Replace them with:  --[--]----------- and --[--]-----[--]--
      if (index < this.intervals.size() && this.alg.startsBefore(this.intervals.get(index), window)) {
        final var prefix = this.alg.intersect(this.intervals.get(index), this.alg.lowerBoundsOf(window));
        final var suffix = this.alg.intersect(this.intervals.get(index), this.alg.upperBoundsOf(window));

        this.intervals.set(index, prefix);
        index += 1;

        if (!this.alg.isEmpty(suffix)) this.intervals.add(index, suffix);
        // The suffix might also be clipped by the next window.
      }

      // Remove any windows contained by this window.
      // Handle these cases: --<---[---]--->--
      // Replace them with:  -----------------
      while (index < this.intervals.size() && !this.alg.endsAfter(this.intervals.get(index), window)) {
        this.intervals.remove(index);
      }

      // Clip the window at the end of this range.
      // Handle these cases: --<---[--->---]--
      // Replace them with:  -----------[--]--
      if (index < this.intervals.size() && !this.alg.startsStrictlyAfter(this.intervals.get(index), window)) {
        this.intervals.set(index, this.alg.intersect(this.intervals.get(index), this.alg.upperBoundsOf(window)));
        // This interval might also be clipped by the next window.
      }
    }
  }


  public void intersectWith(final I interval) {
    if (this.alg.isEmpty(interval)) {
      this.intervals.clear();
    } else {
      this.intersectWithAll(List.of(interval));
    }
  }

  public void intersectWithAll(final IntervalMap<Alg, I> other) {
    this.intersectWithAll(other.intervals);
  }

  /**
   * PRECONDITION: `other` produces non-empty, non-overlapping intervals in ascending order.
   */
  private void intersectWithAll(final Iterable<I> other) {
    int index = 0;

    for (final var window : other) {
      // Remove any initial windows that don't intersect this one
      while (index < this.intervals.size() && this.alg.endsBefore(this.intervals.get(index), window)) {
        this.intervals.remove(index);
      }

      // Clip the first window intersecting this one.
      if (index < this.intervals.size() && this.alg.startsBefore(this.intervals.get(index), window)) {
        final var original = this.intervals.get(index);
        final var remainder = this.alg.intersect(original, this.alg.upperBoundsOf(window));
        this.intervals.set(index, this.alg.intersect(original, window));
        index += 1;
        if (!this.alg.isEmpty(remainder)) this.intervals.add(index, remainder);
      }

      // Keep any windows contained within this one.
      while (index < this.intervals.size() && !this.alg.endsAfter(this.intervals.get(index), window)) {
        index += 1;
      }

      // Clip the window at the end of this range.
      if (index < this.intervals.size() && !this.alg.startsAfter(this.intervals.get(index), window)) {
        final var original = this.intervals.get(index);
        final var remainder = this.alg.intersect(original, this.alg.upperBoundsOf(window));
        this.intervals.set(index, this.alg.intersect(original, window));
        index += 1;
        if (!this.alg.isEmpty(remainder)) this.intervals.add(index, remainder);
      }
    }

    // Remove any remaining windows, since they're after everything in `other`.
    while (index < this.intervals.size()) {
      this.intervals.remove(index);
    }
  }


  // TODO: implement symmetric difference `negateUnder()`

  public boolean isEmpty() {
    return new IntervalMap<>(this.alg).includesAll(this);
  }

  public int size(){
    return intervals.size();
  }

  public boolean includes(final I interval) {
    if (this.alg.isEmpty(interval)) return true;
    return this.includesAll(List.of(interval));
  }

  public boolean includesAll(final IntervalMap<Alg, I> other) {
    return this.includesAll(other.intervals);
  }

  /**
   * PRECONDITION: `other` produces non-empty, non-overlapping intervals in ascending order.
   */
  private boolean includesAll(final Iterable<I> other) {
    int index = 0;

    for (final var window : other) {
      // Skip any windows that fully precede this one.
      while (index < this.intervals.size() && this.alg.endsStrictlyBefore(this.intervals.get(index), window)) {
        index += 1;
      }

      // If windows.get(index) doesn't contain `window`, then nothing does.
      if (index >= this.intervals.size() || !this.alg.contains(this.intervals.get(index), window)) {
        return false;
      }
    }

    return true;
  }

  public Iterable<I> ascendingOrder() {
    // SAFETY: calling `.remove()` on the returned iterator does not breach encapsulation.
    // The same effect can be achieved by calling `windows.subtract()` against the data returned by the iterator,
    // except for the added burden of avoiding `ConcurrentModificationException`s.
    return this.intervals;
  }

  public Iterable<I> descendingOrder() {
    return () -> new Iterator<>() {
      private final ListIterator<I> iter = IntervalMap.this.intervals.listIterator(IntervalMap.this.intervals.size());

      @Override
      public boolean hasNext() {
        return this.iter.hasPrevious();
      }

      @Override
      public I next() {
        return this.iter.previous();
      }
    };
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof IntervalMap)) return false;
    final var other = (IntervalMap<?, ?>) obj;

    return Objects.equals(this.intervals, other.intervals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.intervals);
  }

  @Override
  public String toString() {
    return this.intervals.toString();
  }
}
