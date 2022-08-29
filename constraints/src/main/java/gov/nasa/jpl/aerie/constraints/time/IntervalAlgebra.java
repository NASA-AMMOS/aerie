package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;

public class IntervalAlgebra {

  private IntervalAlgebra() {}

  public static boolean isEmpty(Interval x) {
    return x.isEmpty();
  }

  public static Interval unify(final Interval x, final Interval y) {
    return Interval.unify(x, y);
  }

  public static Interval intersect(final Interval x, final Interval y) {
    return Interval.intersect(x, y);
  }

  public static Interval lowerBoundsOf(final Interval x) {
    if (x.isEmpty()) return Interval.FOREVER;
    return Interval.between(
        Duration.MIN_VALUE,
        Inclusive, x.start,
        x.startInclusivity.opposite()
    );
  }

  public static Interval upperBoundsOf(final Interval x) {
    if (x.isEmpty()) return Interval.FOREVER;
    return Interval.between(
        x.end,
        x.endInclusivity.opposite(), Duration.MAX_VALUE,
        Inclusive
    );
  }

  static boolean overlaps(Interval x, Interval y) {
    return !isEmpty(intersect(x, y));
  }
  static boolean contains(Interval outer, Interval inner) {
    // If `inner` doesn't overlap with the complement of `outer`,
    // then `inner` must exist entirely within `outer`.
    return !(overlaps(inner, upperBoundsOf(outer)) || overlaps(inner, lowerBoundsOf(outer)));
  }
  static boolean strictlyContains(Interval outer, Interval inner) {
    return contains(outer, inner) && !contains(inner, outer);
  }
  static boolean equals(Interval x, Interval y) {
    return contains(x, y) && contains(y, x);
  }

  static boolean startsBefore(Interval x, Interval y) {
    return strictlyContains(lowerBoundsOf(y), lowerBoundsOf(x));
  }
  static boolean endsAfter(Interval x, Interval y) {
    return strictlyContains(upperBoundsOf(y), upperBoundsOf(x));
  }

  static boolean startsAfter(Interval x, Interval y) {
    return endsBefore(y, x);
  }
  static boolean endsBefore(Interval x, Interval y) {
    return endsStrictlyBefore(x, y) || meets(x, y);
  }

  static boolean endsStrictlyBefore(Interval x, Interval y) {
    return !isEmpty(intersect(upperBoundsOf(x), lowerBoundsOf(y)));
  }
  static boolean startsStrictlyAfter(Interval x, Interval y) {
    return endsStrictlyBefore(y, x);
  }

  static boolean meets(Interval x, Interval y) {
    return equals(upperBoundsOf(x), upperBoundsOf(lowerBoundsOf(y)));
  }
  static boolean isMetBy(Interval x, Interval y) {
    return meets(y, x);
  }
}
