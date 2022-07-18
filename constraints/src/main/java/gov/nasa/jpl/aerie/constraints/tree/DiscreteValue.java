package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DiscreteValue implements Expression<DiscreteProfile> {
  public final SerializedValue value;

  public DiscreteValue(final SerializedValue value) {
    this.value = value;
  }

  @Override
  public DiscreteProfile evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    return new DiscreteProfile(List.of(new DiscreteProfilePiece(bounds, this.value)));
  }

  @Override
  public void extractResources(final Set<String> names) { }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(value %s)",
        prefix,
        this.value
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DiscreteValue)) return false;
    final var o = (DiscreteValue)obj;

    return Objects.equals(this.value, o.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.value);
  }
}
