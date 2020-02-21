package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

import java.util.Objects;

public final class Window {
  // TODO: Find a good way to represent windows with infinite bounds.
  //   For instance, the window representing all time should be representable.
  // TODO: Find a good way to represent the empty window.
  public final Instant start;
  public final Instant end;

  private Window(final Instant start, final Instant end) {
    this.start = Objects.requireNonNull(start);
    this.end = Objects.requireNonNull(end);
  }

  public static Window between(final Instant start, final Instant end) {
    return new Window(start, end);
  }

  public boolean overlaps(final Window other) {
    return this.end.compareTo(other.start) >= 0 && other.end.compareTo(this.start) >= 0;
  }

  /**
   * Returns the largest window contained by both `x` and `y`.
   */
  public static Window greatestLowerBound(final Window x, final Window y) {
    if (!x.overlaps(y)) {
      // TODO: Find a good way to represent an empty interval.
      return null;
    } else {
      return Window.between(Instant.max(x.start, y.start), Instant.min(x.end, y.end));
    }
  }

  /**
   * Returns the smallest window containing both `x` and `y`.
   */
  public static Window leastUpperBound(final Window x, final Window y) {
    return Window.between(Instant.min(x.start, y.start), Instant.max(x.end, y.end));
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Window)) return false;
    final var other = (Window)o;

    return ( Objects.equals(this.start, other.start)
        &&   Objects.equals(this.end, other.end));
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.start, this.end);
  }

  @Override
  public String toString() {
    return "Window(" + this.start + " .. " + this.end + ")";
  }
}
