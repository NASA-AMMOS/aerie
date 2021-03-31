package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;

import java.util.List;
import java.util.Map;

public final class DiscreteValue implements Expression<DiscreteProfile> {
  private final SerializedValue value;

  public DiscreteValue(final SerializedValue value) {
    this.value = value;
  }

  @Override
  public DiscreteProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return new DiscreteProfile(List.of(new DiscreteProfilePiece(results.bounds, this.value)));
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(value %s)",
        prefix,
        this.value
    );
  }
}
