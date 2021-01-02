package gov.nasa.jpl.aerie.time;

import java.util.Objects;

public final class Window {
  // If end.shorterThan(start), this is the empty window.
  // If end.equals(start), this is a single point.
  // If end.longerThan(start), this is a closed interval.
  // We don't seem to be alone in this representation -- the interval arithmetic library Gaol
  //   represents empty intervals in the same way.
  public final Duration start;
  public final Duration end;

  private Window(final Duration start, final Duration end) {
    this.start = Objects.requireNonNull(start);
    this.end = Objects.requireNonNull(end);
  }

  public static Window between(final Duration start, final Duration end) {
    if (end.shorterThan(start)) throw new RuntimeException("The end of a window cannot come before its start");
    return new Window(start, end);
  }

  public static Window between(final long start, final long end, final Duration unit) {
    return between(Duration.of(start, unit), Duration.of(end, unit));
  }

  public static Window window(final Duration start, final Duration end) {
    return between(start, end);
  }

  public static Window window(final long start, final long end, final Duration unit) {
    return between(start, end, unit);
  }

  public static Window at(final Duration point) {
    return new Window(point, point);
  }

  public static Window at(final long quantity, final Duration unit) {
    return at(Duration.of(quantity, unit));
  }

  public static Window roundOut(final double start, final double end, final Duration unit) {
    return between(Duration.roundDownward(start, unit), Duration.roundUpward(end, unit));
  }

  public static Window roundIn(final double start, final double end, final Duration unit) {
    return between(Duration.roundUpward(start, unit), Duration.roundDownward(end, unit));
  }

  public static final Window EMPTY = new Window(Duration.ZERO, Duration.ZERO.minus(Duration.EPSILON));
  public static final Window FOREVER = new Window(Duration.MIN_VALUE, Duration.MAX_VALUE);

  public boolean isEmpty() {
    return this.end.shorterThan(this.start);
  }

  public boolean overlaps(final Window other) {
    return !other.isEmpty() && !this.isEmpty() && !this.end.shorterThan(other.start) && !other.end.shorterThan(this.start);
  }

  public boolean contains(final Window other) {
    return other.isEmpty() || (!this.isEmpty() && !this.end.shorterThan(other.end) && !other.start.shorterThan(this.start));
  }

  public Duration duration() {
    if (this.isEmpty()) return Duration.ZERO;
    return this.end.minus(this.start);
  }

  /**
   * Returns the largest window contained by both `x` and `y`.
   */
  public static Window greatestLowerBound(final Window x, final Window y) {
    return new Window(Duration.max(x.start, y.start), Duration.min(x.end, y.end));
  }

  /**
   * Returns the smallest window containing both `x` and `y`.
   */
  public static Window leastUpperBound(final Window x, final Window y) {
    return new Window(Duration.min(x.start, y.start), Duration.max(x.end, y.end));
  }

  /**
   * Returns the largest window containing `self` but not `other`.
   */
  public static Window subtract(final Window self, final Window other) {
    if (other.end.shorterThan(other.start)) {
      // Trivial intersection: nothing to subtract.
      return other;
    } else if (self.end.shorterThan(self.start)) {
      // Trivial intersection: subtract everything! (Of which there are no things.)
      return self;
    } else if (self.end.shorterThan(other.start) || other.end.shorterThan(self.start)) {
      // Trivial intersections: no overlap.
      return self;
    } else {
      // The intervals non-trivially intersect.
      if (self.start.shorterThan(other.start) && other.end.shorterThan(self.end)) {
        // This interval fully contains the other, splitting into two disjoint intervals.
        // The largest interval containing these intervals is the empty interval.
        return Window.EMPTY;
      } else if (!other.start.longerThan(self.start) && !self.start.longerThan(other.end) && other.end.shorterThan(self.end)) {
        // This interval is cut on the left by the other.
        return Window.between(other.end.plus(Duration.EPSILON), self.end);
      } else if (self.start.shorterThan(other.start) && !other.start.longerThan(self.end) && !self.end.longerThan(other.end)) {
        // This interval is cut on the right by the other.
        return Window.between(self.start, other.start.minus(Duration.EPSILON));
      } else /* other.start <= self.start && this.end <= other.end */ {
        // This interval is fully contained by the other.
        return Window.EMPTY;
      }
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Window)) return false;
    final var other = (Window)o;

    return ( (this.isEmpty() && other.isEmpty())
          || ( Objects.equals(this.start, other.start)
            && Objects.equals(this.end, other.end) ) );
  }

  @Override
  public int hashCode() {
    return (this.isEmpty()) ? Objects.hash(0L, -1L) : Objects.hash(this.start, this.end);
  }

  @Override
  public String toString() {
    if (this.isEmpty()) {
      return "Window(empty)";
    } else if (this.start.equals(this.end)) {
      return String.format("Window(at: %s)", this.start);
    } else {
      return String.format("Window(from: %s, to: %s)", this.start, this.end);
    }
  }
}
