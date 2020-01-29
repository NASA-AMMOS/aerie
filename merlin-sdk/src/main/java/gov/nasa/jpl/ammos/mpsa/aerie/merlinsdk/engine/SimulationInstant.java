package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration2;
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

  public static SimulationInstant fromQuantity(final long quantity, final TimeUnit units) {
    switch (units) {
      case MICROSECONDS: return new SimulationInstant(quantity);
      case MILLISECONDS: return new SimulationInstant(quantity * 1000L);
      case SECONDS:      return new SimulationInstant(quantity * 1000000L);
      case MINUTES:      return new SimulationInstant(quantity * 1000000L * 60L);
      case HOURS:        return new SimulationInstant(quantity * 1000000L * 60L * 60L);
      case DAYS:         return new SimulationInstant(quantity * 1000000L * 60L * 60L * 24L);
      case WEEKS:        return new SimulationInstant(quantity * 1000000L * 60L * 60L * 24L * 7L);
      default: throw new Error("Unknown TimeUnit value: " + units);
    }
  }

  @Override
  public SimulationInstant plus(final Duration2 duration) {
    return new SimulationInstant(Math.addExact(this.microsecondsFromStart, duration.durationInMicroseconds));
  }

  @Override
  public SimulationInstant minus(final Duration2 duration) {
    return new SimulationInstant(Math.subtractExact(this.microsecondsFromStart, duration.durationInMicroseconds));
  }

  @Override
  public Duration2 durationFrom(final Instant other) {
    return Duration2.fromQuantity(
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
