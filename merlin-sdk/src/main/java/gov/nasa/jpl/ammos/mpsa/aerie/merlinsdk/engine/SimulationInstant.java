package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

public final class SimulationInstant implements Instant {
  // Range of -2^63 to 2^63 - 1.
  // This comes out to almost 600,000 years, at microsecond resolution.
  // Merlin was not designed for time scales longer than this.
  private final long microsecondsFromStart;

  private SimulationInstant(final long microsecondsFromStart) {
    this.microsecondsFromStart = microsecondsFromStart;
  }

  public static SimulationInstant origin() {
    return new SimulationInstant(0);
  }

  @Override
  public Instant plus(final Duration duration) {
    return new SimulationInstant(Math.addExact(this.microsecondsFromStart, duration.durationInMicroseconds));
  }

  @Override
  public Instant minus(final Duration duration) {
    return new SimulationInstant(Math.subtractExact(this.microsecondsFromStart, duration.durationInMicroseconds));
  }

  @Override
  public Duration durationFrom(final Instant other) {
    return Duration.fromQuantity(
        Math.subtractExact(this.microsecondsFromStart, ((SimulationInstant)other).microsecondsFromStart),
        TimeUnit.MICROSECONDS);
  }

  @Override
  public int compareTo(final Instant other) {
    return Long.compare(this.microsecondsFromStart, ((SimulationInstant)other).microsecondsFromStart);
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
    return "" + Long.toUnsignedString(this.microsecondsFromStart) + "µs";
  }
}
