package gov.nasa.jpl.aerie.merlin.protocol.types;

import java.util.Objects;

public final class DelimitedDynamics<Dynamics> {
  public final Duration extent;
  public final Dynamics dynamics;

  public boolean isPersistent() {
    return (this.extent.isEqualTo(Duration.MAX_VALUE));
  }

  private DelimitedDynamics(final Duration extent, final Dynamics dynamics) {
    this.extent = Objects.requireNonNull(extent);
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
    return String.format("(%s, %s)", this.extent, this.dynamics);
  }
}
