package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

public final class SimulationInstant {
  // Range of -2^63 to 2^63 - 1.
  // This comes out to almost 600,000 years, at microsecond resolution.
  // Merlin was not designed for time scales longer than this.
  private final long microsecondsFromStart;

  private SimulationInstant(final long microsecondsFromStart) {
    this.microsecondsFromStart = microsecondsFromStart;
  }

  public static final SimulationInstant ORIGIN = new SimulationInstant(0);

  public SimulationInstant plus(final Duration duration) {
    return new SimulationInstant(Math.addExact(this.microsecondsFromStart, duration.in(Duration.MICROSECONDS)));
  }

  public SimulationInstant minus(final Duration duration) {
    return new SimulationInstant(Math.subtractExact(this.microsecondsFromStart, duration.in(Duration.MICROSECONDS)));
  }

  public Duration durationFrom(final SimulationInstant other) {
    return Duration.MICROSECOND.times(Math.subtractExact(this.microsecondsFromStart, other.microsecondsFromStart));
  }

  public int compareTo(final SimulationInstant other) {
    return Long.compare(this.microsecondsFromStart, other.microsecondsFromStart);
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SimulationInstant)) return false;
    final var other = (SimulationInstant)o;

    return (this.microsecondsFromStart == other.microsecondsFromStart);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.microsecondsFromStart);
  }

  @Override
  public String toString() {
    return "" + this.microsecondsFromStart + "µs";
  }

  public static final class Trait implements Instant<SimulationInstant> {
    @Override
    public SimulationInstant origin() {
      return SimulationInstant.ORIGIN;
    }

    @Override
    public SimulationInstant plus(final SimulationInstant time, final Duration duration) {
      return time.plus(duration);
    }

    @Override
    public SimulationInstant minus(final SimulationInstant time, final Duration duration) {
      return time.minus(duration);
    }

    @Override
    public Duration minus(final SimulationInstant end, final SimulationInstant start) {
      return end.durationFrom(start);
    }

    @Override
    public int compare(final SimulationInstant left, final SimulationInstant right) {
      return left.compareTo(right);
    }
  }
}
