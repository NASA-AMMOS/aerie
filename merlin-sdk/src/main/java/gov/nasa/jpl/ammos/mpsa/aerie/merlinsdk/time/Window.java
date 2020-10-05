package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

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

  public static Window between(final long startQuantity, final Duration startUnit, final long endQuantity, final Duration endUnit) {
    return between(Duration.of(startQuantity, startUnit), Duration.of(endQuantity, endUnit));
  }

  public static Window window(final Duration start, final Duration end) {
    return between(start, end);
  }

  public static Window window(final long startQuantity, final Duration startUnit, final long endQuantity, final Duration endUnit) {
    return between(startQuantity, startUnit, endQuantity, endUnit);
  }

  public static Window at(final Duration point) {
    return new Window(point, point);
  }

  public static Window at(final long quantity, final Duration unit) {
    return at(Duration.of(quantity, unit));
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
