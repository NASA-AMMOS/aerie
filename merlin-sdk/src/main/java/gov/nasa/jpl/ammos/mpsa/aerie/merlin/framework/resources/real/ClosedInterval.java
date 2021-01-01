package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.real;

public final class ClosedInterval {
  // If end.shorterThan(start), this is the empty interval.
  // If end.equals(start), this is a single point.
  // If end.longerThan(start), this is an interval of non-zero measure.
  public final double min;
  public final double max;

  private ClosedInterval(final double min, final double max) {
    this.min = min;
    this.max = max;
  }

  public static ClosedInterval between(final double min, final double max) {
    if (max < min) throw new RuntimeException("The min of an interval cannot come before its max");
    return new ClosedInterval(min, max);
  }

  public static ClosedInterval at(final double value) {
    return ClosedInterval.between(value, value);
  }

  public static final ClosedInterval EMPTY =
      new ClosedInterval(0, -1);
  public static final ClosedInterval EVERYTHING =
      new ClosedInterval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

  public boolean isEmpty() {
    return this.max < this.min;
  }

  public boolean overlaps(final ClosedInterval other) {
    return !other.isEmpty() && !this.isEmpty() && (this.min <= other.max) && (other.min <= this.max);
  }

  public boolean contains(final ClosedInterval other) {
    return other.isEmpty() || (!this.isEmpty() && (this.min <= other.min) && (other.max <= this.max));
  }

  public ClosedInterval minus(final ClosedInterval other) {
    if (other.max < other.min) {
      // Trivial intersection: nothing to subtract.
      return other;
    } else if (this.max < this.min) {
      // Trivial intersection: subtract everything! (Of which there are no things.)
      return this;
    } else if (this.max < other.min || other.max < this.min) {
      // Trivial intersections: no overlap.
      return this;
    } else {
      // The intervals non-trivially intersect.
      if (this.min < other.min && other.max < this.max) {
        // This interval fully contains the other, splitting into two disjoint intervals.
        // The largest interval containing these intervals is the empty interval.
        return ClosedInterval.EMPTY;
      } else if (other.min <= this.min && this.min <= other.max && other.max < this.max) {
        // This interval is cut on the left by the other.
        return ClosedInterval.between(Math.nextUp(other.max), this.max);
      } else if (this.min < other.min && other.min <= this.max && this.max <= other.max) {
        // This interval is cut on the right by the other.
        return ClosedInterval.between(this.min, Math.nextDown(other.min));
      } else /* other.min <= this.min && this.max <= other.max */ {
        // This interval is fully contained by the other.
        return ClosedInterval.EMPTY;
      }
    }
  }

  /**
   * Returns the largest window contained by both `x` and `y`.
   */
  public static ClosedInterval greatestLowerBound(final ClosedInterval x, final ClosedInterval y) {
    return new ClosedInterval(Math.max(x.min, y.min), Math.min(x.max, y.max));
  }

  /**
   * Returns the smallest window containing both `x` and `y`.
   */
  public static ClosedInterval leastUpperBound(final ClosedInterval x, final ClosedInterval y) {
    return new ClosedInterval(Math.min(x.min, y.min), Math.max(x.max, y.max));
  }
}
