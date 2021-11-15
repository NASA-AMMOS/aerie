package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Objects;

public final class DiscreteProfilePiece {
  public final Window window;
  public final SerializedValue value;

  public DiscreteProfilePiece(final Window window, final SerializedValue value) {
    this.window = window;
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DiscreteProfilePiece)) return false;
    final var other = (DiscreteProfilePiece)obj;

    return this.window.equals(other.window) &&
           this.value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.window, this.value);
  }
}
