package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

import java.util.Objects;

public final class Window {
  public final Instant start;
  public final Instant end;

  private Window(final Instant start, final Instant end) {
    this.start = Objects.requireNonNull(start);
    this.end = Objects.requireNonNull(end);
  }

  public static Window between(final Instant start, final Instant end) {
    return new Window(start, end);
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
