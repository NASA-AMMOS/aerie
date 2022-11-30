package gov.nasa.jpl.aerie.constraints.profile;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.IntervalAlgebra;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
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
public final class IntervalMap<V> implements Iterable<Segment<V>>, Profile<V> {
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
  public Stream<Segment<V>> stream(final Interval bounds) {
    return stream()
        .filter($ -> !Interval.intersect($.interval(), bounds).isEmpty())
        .map(s -> s.mapInterval(i -> Interval.intersect(i, bounds)));
  }

  @Override
  public IntervalMap<V> evaluate(final Interval bounds) {
    if (bounds.contains(this.segments.get(0).interval()) && bounds.contains(this.segments.get(size()).interval())) {
      return this;
    } else {
      return Profile.super.evaluate(bounds);
    }
  }

  public Optional<Pair<Duration, Interval.Inclusivity>> firstTimeEqualTo(final V value) {
    for (final var segment: this.segments) {
      if (segment.value().equals(value)) {
        final var window = segment.interval();
        return Optional.of(Pair.of(window.start, window.startInclusivity));
      }
    }
    return Optional.empty();
  }

  public Optional<Pair<Duration, Interval.Inclusivity>> lastTimeEqualTo(final V value) {
    for (int i = this.segments.size() - 1; i >= 0; i--) {
      final var segment = this.segments.get(i);
      if (segment.value().equals(value)) {
        final var window = segment.interval();
        return Optional.of(Pair.of(window.end, window.endInclusivity));
      }
    }
    return Optional.empty();
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
            final var newInterval = Interval.between(interval.end, interval.endInclusivity.opposite(), existingInterval.end, existingInterval.endInclusivity);
            if (!newInterval.isEmpty()) {
              this.segments.add(i, Segment.of(newInterval, segment.value()));
            } else {
              i--;
            }
          }
        } else {
          final var segment = this.segments.remove(i);
          final var leftInterval = Interval.between(existingInterval.start, existingInterval.startInclusivity, interval.start, interval.startInclusivity.opposite());
          if (!leftInterval.isEmpty()) {
            this.segments.add(i, Segment.of(leftInterval, segment.value()));
          } else {
            i--;
          }
          if (IntervalAlgebra.endsAfter(existingInterval, interval)) {
            final var rightInterval = Interval.between(interval.end, interval.endInclusivity.opposite(), existingInterval.end, existingInterval.endInclusivity);
            if (!rightInterval.isEmpty()) {
              this.segments.add(i+1, Segment.of(rightInterval, segment.value()));
            } else {
              i--;
            }
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
