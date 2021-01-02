package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.utilities.IntervalAlgebra;
import gov.nasa.jpl.aerie.utilities.IntervalSet;

import java.util.List;
import java.util.Objects;

/*
 * A condition on the reals is a compact set -- a finite union of closed intervals.
 */
public final class RealCondition {
  private final IntervalSet<ClosedIntervalAlgebra, ClosedInterval> intervals =
      new IntervalSet<>(new ClosedIntervalAlgebra());

  public RealCondition() {}

  public RealCondition(final RealCondition other) {
    this.intervals.addAll(other.intervals);
  }

  public RealCondition(final List<ClosedInterval> intervals) {
    for (final var interval : intervals) this.add(interval);
  }

  public RealCondition(final ClosedInterval... intervals) {
    for (final var interval : intervals) this.add(interval);
  }


  public void add(final ClosedInterval interval) {
    this.intervals.add(interval);
  }

  public void addAll(final RealCondition other) {
    this.intervals.addAll(other.intervals);
  }

  public static RealCondition union(final RealCondition left, final RealCondition right) {
    final var result = new RealCondition(left);
    result.addAll(right);
    return result;
  }


  public void subtract(final ClosedInterval interval) {
    this.intervals.subtract(interval);
  }

  public void subtractAll(final RealCondition other) {
    this.intervals.subtractAll(other.intervals);
  }

  public static RealCondition minus(final RealCondition left, final RealCondition right) {
    final var result = new RealCondition(left);
    result.subtractAll(right);
    return result;
  }


  public void intersectWith(final ClosedInterval interval) {
    this.intervals.intersectWith(interval);
  }

  public void intersectWith(final RealCondition other) {
    this.intervals.intersectWithAll(other.intervals);
  }

  public static RealCondition intersection(final RealCondition left, final RealCondition right) {
    final var result = new RealCondition(left);
    result.intersectWith(right);
    return result;
  }


  public boolean isEmpty() {
    return this.intervals.isEmpty();
  }


  public boolean includes(final ClosedInterval probe) {
    return this.intervals.includes(probe);
  }

  public boolean includes(final RealCondition other) {
    return this.intervals.includesAll(other.intervals);
  }

  public boolean includesPoint(final double value) {
    return this.intervals.includes(ClosedInterval.at(value));
  }


  public Iterable<ClosedInterval> ascendingOrder() {
    return this.intervals.ascendingOrder();
  }
  public Iterable<ClosedInterval> descendingOrder() {
    return this.intervals.descendingOrder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof RealCondition)) return false;
    final var other = (RealCondition) obj;

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

  private static class ClosedIntervalAlgebra implements IntervalAlgebra<ClosedIntervalAlgebra, ClosedInterval> {
    @Override
    public boolean isEmpty(final ClosedInterval x) {
      return x.isEmpty();
    }

    @Override
    public ClosedInterval unify(final ClosedInterval x, final ClosedInterval y) {
      return ClosedInterval.leastUpperBound(x, y);
    }

    @Override
    public ClosedInterval intersect(final ClosedInterval x, final ClosedInterval y) {
      return ClosedInterval.greatestLowerBound(x, y);
    }

    @Override
    public ClosedInterval lowerBoundsOf(final ClosedInterval x) {
      return ClosedInterval.between(
          Double.NEGATIVE_INFINITY,
          (x.isEmpty()) ? Double.POSITIVE_INFINITY : Math.nextDown(x.min));
    }

    @Override
    public ClosedInterval upperBoundsOf(final ClosedInterval x) {
      return ClosedInterval.between(
          (x.isEmpty()) ? Double.NEGATIVE_INFINITY : Math.nextUp(x.max),
          Double.POSITIVE_INFINITY);
    }

    @Override
    public Relation relationBetween(final ClosedInterval x, final ClosedInterval y) {
      /*
        y -----|-----  **************
        x  |           * Before
               |       * Equals
                   |   * After
           [   ]       * Contains
           [       ]   * Contains
               [   ]   * Contains

        y ---[---]---  **************
        x  |           * Before
             [   ]     * Equals
                   |   * After
             |         * ContainedBy
             [ ]       * ContainedBy
               |       * ContainedBy
               [ ]     * ContainedBy
                 |     * ContainedBy
           [ ]         * LeftOverhang
           [   ]       * LeftOverhang
           [     ]     * Contains
           [       ]   * Contains
             [     ]   * Contains
               [   ]   * RightOverhang
                 [ ]   * RightOverhang
                       **************
      */

      if (x.isEmpty()) return (y.isEmpty()) ? Relation.Equals : Relation.ContainedBy;
      if (y.isEmpty()) return Relation.Contains;

      if (y.min == x.min && x.max == y.max) return Relation.Equals;
      if (x.max < y.min) return Relation.Before;
      if (y.max < x.min) return Relation.After;

      if (y.min <= x.min && x.max <= y.max) return Relation.ContainedBy;
      if (x.max < y.max) return Relation.LeftOverhang;
      if (y.min < x.min) return Relation.RightOverhang;

      return Relation.Contains;
    }
  }
}
