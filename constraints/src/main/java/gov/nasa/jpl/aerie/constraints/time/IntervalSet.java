package gov.nasa.jpl.aerie.constraints.time;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

public class IntervalSet<Alg, I> {
  private final IntervalAlgebra<Alg, I> alg;

  // INVARIANT: `intervals` is list of non-empty, non-overlapping intervals in ascending order.
  private final List<I> intervals = new ArrayList<>();

  public IntervalSet(final IntervalAlgebra<Alg, I> algebra) {
    this.alg = Objects.requireNonNull(algebra);
  }

  public IntervalSet(final IntervalSet<Alg, I> other) {
    this.alg = other.alg;
    this.intervals.addAll(other.intervals);
  }


  public void add(final I interval) {
    if (this.alg.isEmpty(interval)) return;

    // OPTIMIZATION: If this interval fits at the end of our list, just do that.
    // Common case for building up a set of intervals.
    // This whole clause can be removed without affecting correctness.
    if (
        this.intervals.size() == 0 ||
        this.alg.endsStrictlyBefore(this.intervals.get(this.intervals.size() - 1), interval)
    ) {
      this.intervals.add(interval);
      return;
    } else if (!this.alg.startsBefore(interval, this.intervals.get(this.intervals.size() - 1))) {
      this.intervals.set(
          this.intervals.size() - 1,
          this.alg.unify(interval, this.intervals.get(this.intervals.size() - 1)));
      return;
    }

    this.addAll(List.of(interval));
  }

  public void addAll(final IntervalSet<Alg, I> other) {
    this.addAll(other.intervals);
  }

  /**
   * PRECONDITION: `other` produces non-empty, non-overlapping intervals in ascending order.
   */
  private void addAll(final Iterable<I> other) {
    int index = 0;

    for (final var window : other) {
      // Skip windows that end before this one starts.
      while (
          index < this.intervals.size() &&
          this.alg.endsStrictlyBefore(this.intervals.get(index), window)
      ) {
        index += 1;
      }

      // Remove and join with any windows that overlap this window.
      var joined = window;
      while (
          index < this.intervals.size() &&
          !this.alg.endsStrictlyBefore(window, this.intervals.get(index))
      ) {
        joined = this.alg.unify(joined, this.intervals.remove(index));
      }

      this.intervals.add(index, joined);
    }
  }


  public void subtract(final I interval) {
    if (this.alg.isEmpty(interval)) return;
    this.subtractAll(List.of(interval));
  }

  public void subtractAll(final IntervalSet<Alg, I> other) {
    this.subtractAll(other.intervals);
  }

  /**
   * PRECONDITION: `other` produces non-empty, non-overlapping intervals in ascending order.
   */
  private void subtractAll(final Iterable<I> other) {
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

  public void intersectWithAll(final IntervalSet<Alg, I> other) {
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
    return new IntervalSet<>(this.alg).includesAll(this);
  }


  public boolean includes(final I interval) {
    if (this.alg.isEmpty(interval)) return true;
    return this.includesAll(List.of(interval));
  }

  public boolean includesAll(final IntervalSet<Alg, I> other) {
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
      private final ListIterator<I> iter = IntervalSet.this.intervals.listIterator();

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
    if (!(obj instanceof IntervalSet)) return false;
    final var other = (IntervalSet<?, ?>) obj;

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
