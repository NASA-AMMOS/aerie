package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.time.Duration;

import java.util.Objects;

public final class DelimitedDynamics<Dynamics> {
  public final Duration endTime;
  public final Dynamics dynamics;

  private DelimitedDynamics(final Duration endTime, final Dynamics dynamics) {
    this.endTime = Objects.requireNonNull(endTime);
    this.dynamics = Objects.requireNonNull(dynamics);
  }

  public static <Dynamics> DelimitedDynamics<Dynamics> delimited(final Duration endTime, final Dynamics dynamics) {
    return new DelimitedDynamics<>(endTime, dynamics);
  }

  public static <Dynamics> DelimitedDynamics<Dynamics> persistent(final Dynamics dynamics) {
    return new DelimitedDynamics<>(Duration.MAX_VALUE, dynamics);
  }

  @Override
  public final String toString() {
    return String.format("(%s, %s)", this.endTime, this.dynamics);
  }
}
