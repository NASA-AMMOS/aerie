package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Objects;

public final class DiscreteProfilePiece {
  public final Interval interval;
  public final SerializedValue value;

  public DiscreteProfilePiece(final Interval interval, final SerializedValue value) {
    this.interval = interval;
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DiscreteProfilePiece)) return false;
    final var other = (DiscreteProfilePiece)obj;

    return this.interval.equals(other.interval) &&
           this.value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.interval, this.value);
  }
}
