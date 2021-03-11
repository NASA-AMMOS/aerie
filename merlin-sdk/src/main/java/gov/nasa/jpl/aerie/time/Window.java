package gov.nasa.jpl.aerie.time;

import java.util.Objects;

import static gov.nasa.jpl.aerie.time.Window.Inclusivity.*;

public final class Window {
  // If end.shorterThan(start), this is the empty window.
  // If end.equals(start), this is a single point.
  // If end.longerThan(start), this is a closed interval.
  // We don't seem to be alone in this representation -- the interval arithmetic library Gaol
  //   represents empty intervals in the same way.
  public final Duration start;
  public final Duration end;
  public final Inclusivity startInclusivity;
  public final Inclusivity endInclusivity;

  public enum Inclusivity {
    Inclusive,
    Exclusive;

    Inclusivity opposite() {
      return (this == Inclusive) ? Exclusive : Inclusive;
    }
  }

  public boolean includesStart() {
    return this.startInclusivity == Inclusive;
  }

  public boolean includesEnd() {
    return this.endInclusivity == Inclusive;
  }

  private Window(
      final Duration start,
      final Inclusivity startInclusivity,
      final Duration end,
      final Inclusivity endInclusivity) {
    this.start = Objects.requireNonNull(start);
    this.end = Objects.requireNonNull(end);
    this.startInclusivity = startInclusivity;
    this.endInclusivity = endInclusivity;
  }

  private Window(final Duration start, final Duration end) {
    this(start, Inclusive, end, Inclusive);
  }

  /**
   * Constructs a window between two durations based on a common instant.
   *
   * @param start The starting time of the window.
   * @param end The ending time of the window.
   * @return A non-empty window if start &le; end, or an empty window otherwise.
   */
  public static Window between(
      final Duration start,
      Inclusivity startInclusivity,
      final Duration end,
      Inclusivity endInclusivity
  ) {
    return (end.shorterThan(start))
      ? Window.EMPTY
      : new Window(start, startInclusivity, end, endInclusivity);
  }

  public static Window between(final Duration start, final Duration end) {
    return between(start, Inclusive, end, Inclusive);
  }

  public static Window between(
      final long start,
      final Inclusivity startInclusivity,
      final long end,
      final Inclusivity endInclusivity,
      final Duration unit
  ) {
    return between(Duration.of(start, unit), startInclusivity, Duration.of(end, unit), endInclusivity);
  }

  public static Window between(final long start, final long end, final Duration unit) {
    return between(start, Inclusive, end, Inclusive, unit);
  }

  public static Window window(
      final Duration start,
      final Inclusivity startInclusivity,
      final Duration end,
      final Inclusivity endInclusivity
  ) {
    return between(start, startInclusivity, end, endInclusivity);
  }

  public static Window window(final Duration start, final Duration end) {
    return window(start, Inclusive, end, Inclusive);
  }

  public static Window window(
      final long start,
      final Inclusivity startInclusivity,
      final long end,
      final Inclusivity endInclusivity,
      final Duration unit
  ) {
    return between(start, startInclusivity, end, endInclusivity, unit);
  }

  public static Window window(final long start, final long end, final Duration unit) {
    return window(start, Inclusive, end, Inclusive, unit);
  }

  public static Window at(final Duration point) {
    return new Window(point, Inclusive, point, Inclusive);
  }

  public static Window at(final long quantity, final Duration unit) {
    return at(Duration.of(quantity, unit));
  }

  public static final Window EMPTY = new Window(Duration.ZERO, Duration.ZERO.minus(Duration.EPSILON));
  public static final Window FOREVER = new Window(Duration.MIN_VALUE, Duration.MAX_VALUE);

  public boolean isEmpty() {
    if (this.end.shorterThan(this.start)) return true;
    if (this.end.longerThan(this.start)) return false;

    return !(this.includesStart() && this.includesEnd());
  }

  public Duration duration() {
    if (this.isEmpty()) return Duration.ZERO;
    return this.end.minus(this.start);
  }

  public int compareStartToStart(final Window other) {
    // First, order by absolute time.
    if (!this.start.isEqualTo(other.start)) {
      return this.start.compareTo(other.start);
    }

    // Second, order by whichever one includes the point.
    if (this.includesStart() != other.includesStart()) {
      return (this.includesStart()) ? -1 : 1;
    }

    return 0;
  }

  public int compareStartToEnd(final Window other) {
    // First, order by absolute time.
    if (!this.start.isEqualTo(other.end)) {
      return this.start.compareTo(other.end);
    }

    // Second, order by whichever one includes the point
    if (!other.includesEnd()) return 1;
    if (!this.includesStart()) return 1;

    return 0;
  }

  public int compareEndToStart(final Window other) {
    return -other.compareStartToEnd(this);
  }

  public int compareEndToEnd(final Window other) {
    // First, order by absolute time.
    if (!this.end.isEqualTo(other.end)) {
      return this.end.compareTo(other.end);
    }

    // Second, order by whichever one includes the point
    if (this.includesEnd() != other.includesEnd()) {
      return (this.includesEnd()) ? -1 : 1;
    }

    return 0;
  }

  /**
   * Returns the largest window contained by both `x` and `y`.
   */
  public static Window greatestLowerBound(final Window x, final Window y) {
    final Duration start;
    final Inclusivity startInclusivity;
    final Duration end;
    final Inclusivity endInclusivity;

    if (x.start.longerThan(y.start)) {
      start = x.start;
      startInclusivity = x.startInclusivity;
    } else if (y.start.longerThan(x.start)) {
      start = y.start;
      startInclusivity = y.startInclusivity;
    } else {
      start = x.start;
      startInclusivity = (x.includesStart() && y.includesStart()) ? Inclusive : Exclusive;
    }

    if (x.end.shorterThan(y.end)) {
      end = x.end;
      endInclusivity = x.endInclusivity;
    } else if (y.end.shorterThan(x.end)) {
      end = y.end;
      endInclusivity = y.endInclusivity;
    } else {
      end = x.end;
      endInclusivity = (x.includesEnd() && y.includesEnd()) ? Inclusive : Exclusive;
    }
    return new Window(start, startInclusivity, end, endInclusivity);
  }

  /**
   * Returns the smallest window containing both `x` and `y`.
   */
  public static Window leastUpperBound(final Window x, final Window y) {
    final Duration start;
    final Inclusivity startInclusivity;
    final Duration end;
    final Inclusivity endInclusivity;

    if (x.start.shorterThan(y.start)) {
      start = x.start;
      startInclusivity = x.startInclusivity;
    } else if (y.start.shorterThan(x.start)) {
      start = y.start;
      startInclusivity = y.startInclusivity;
    } else {
      start = x.start;
      startInclusivity = (x.includesStart() || y.includesStart()) ? Inclusive : Exclusive;
    }

    if (x.end.longerThan(y.end)) {
      end = x.end;
      endInclusivity = x.endInclusivity;
    } else if (y.end.longerThan(x.end)) {
      end = y.end;
      endInclusivity = y.endInclusivity;
    } else {
      end = x.end;
      endInclusivity = (x.includesEnd() || y.includesEnd()) ? Inclusive : Exclusive;
    }

    return new Window(start, startInclusivity, end, endInclusivity);
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
      } else if (other.start.noLongerThan(self.start) && self.start.noLongerThan(other.end) && other.end.shorterThan(self.end)) {
        // This interval is cut on the left by the other.
        return Window.between(other.end, other.endInclusivity.opposite(), self.end, self.endInclusivity);
      } else if (self.start.shorterThan(other.start) && other.start.noLongerThan(self.end) && self.end.noLongerThan(other.end)) {
        // This interval is cut on the right by the other.
        return Window.between(self.start, self.startInclusivity, other.start, other.startInclusivity.opposite());
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
    } else {
      return String.format(
          "Window%s%s, %s%s",
          this.includesStart() ? "[" : "(",
          this.start,
          this.end,
          this.includesEnd() ? "]" : ")"
      );
    }
  }
}
