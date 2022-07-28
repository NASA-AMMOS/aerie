package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;

public class IntervalAlgebra {

  private Interval horizon;

  public IntervalAlgebra() {
    horizon = Interval.between(Duration.MIN_VALUE, Duration.MAX_VALUE);
  }

  public IntervalAlgebra(Interval horizon) {
    this.horizon = horizon;
  }

  public final boolean isEmpty(Interval x) {
    return x.isEmpty();
  }

  public final Interval unify(final Interval x, final Interval y) {
    return Interval.unify(x, y);
  }

  public final Interval intersect(final Interval x, final Interval y) {
    return Interval.intersect(x, y);
  }

  public final Interval lowerBoundsOf(final Interval x) {
    if (x.isEmpty()) return Interval.FOREVER;
    return Interval.between(
        horizon.start,
        Inclusive, x.start,
        x.startInclusivity.opposite()
    );
  }

  public final Interval upperBoundsOf(final Interval x) {
    if (x.isEmpty()) return Interval.FOREVER;
    return Interval.between(
        x.end,
        x.endInclusivity.opposite(), horizon.end,
        Inclusive
    );
  }

  public final Interval bottom() {
    return Interval.between(horizon.start, Exclusive, horizon.start, Exclusive);
  }

  public final Interval bounds() { return horizon; }

  boolean overlaps(Interval x, Interval y) {
    return !isEmpty(intersect(x, y));
  }
  boolean contains(Interval outer, Interval inner) {
    // If `inner` doesn't overlap with the complement of `outer`,
    // then `inner` must exist entirely within `outer`.
    return !(overlaps(inner, upperBoundsOf(outer)) || overlaps(inner, lowerBoundsOf(outer)));
  }
  boolean strictlyContains(Interval outer, Interval inner) {
    return contains(outer, inner) && !contains(inner, outer);
  }
  boolean equals(Interval x, Interval y) {
    return contains(x, y) && contains(y, x);
  }

  boolean startsBefore(Interval x, Interval y) {
    return strictlyContains(lowerBoundsOf(y), lowerBoundsOf(x));
  }
  boolean endsAfter(Interval x, Interval y) {
    return strictlyContains(upperBoundsOf(y), upperBoundsOf(x));
  }

  boolean startsAfter(Interval x, Interval y) {
    return endsBefore(y, x);
  }
  boolean endsBefore(Interval x, Interval y) {
    return endsStrictlyBefore(x, y) || meets(x, y);
  }

  boolean endsStrictlyBefore(Interval x, Interval y) {
    return !isEmpty(intersect(upperBoundsOf(x), lowerBoundsOf(y)));
  }
  boolean startsStrictlyAfter(Interval x, Interval y) {
    return endsStrictlyBefore(y, x);
  }

  boolean meets(Interval x, Interval y) {
    return equals(upperBoundsOf(x), upperBoundsOf(lowerBoundsOf(y)));
  }
  boolean isMetBy(Interval x, Interval y) {
    return meets(y, x);
  }
}
