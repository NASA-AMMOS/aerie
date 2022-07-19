package gov.nasa.jpl.aerie.constraints.time;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

final class IntervalMap<Alg, I, V> {
  private final IntervalAlgebra<Alg, I> alg;

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

  public static <Alg, I, V, R>
  IntervalMap<Alg, I, R> map(
      final IntervalMap<Alg, I, V> intervals,
      final Function<Optional<V>, Optional<R>> transform
  ) {
    final var alg = intervals.alg;
    final var segments = new ArrayList<Pair<I, R>>();

    var previous = alg.bottom();
    for (final var segment : intervals.segments) {
      final var gap = alg.intersect(alg.upperBoundsOf(previous), alg.lowerBoundsOf(segment.getKey()));
      if (!alg.isEmpty(gap)) {
        transform.apply(Optional.empty()).ifPresent($ -> segments.add(Pair.of(gap, $)));
      }

      transform.apply(Optional.of(segment.getValue())).ifPresent($ -> segments.add(Pair.of(segment.getKey(), $)));

      previous = segment.getKey();
    }

    {
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
    throw new NotImplementedException();
  }

  private I getInterval(final int index) {
    final var i = (index > 0) ? index : this.segments.size() - index;
    return this.segments.get(i).getKey();
  }

  private V getValue(final int index) {
    final var i = (index > 0) ? index : this.segments.size() - index;
    return this.segments.get(i).getValue();
  }
}
