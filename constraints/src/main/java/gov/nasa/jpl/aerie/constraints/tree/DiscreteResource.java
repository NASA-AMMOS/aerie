package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.Map;

public final class DiscreteResource implements Expression<DiscreteProfile> {
  public final String name;

  public DiscreteResource(final String name) {
    this.name = name;
  }

  @Override
  public DiscreteProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return results.discreteProfiles.get(this.name);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(resource %s)",
        prefix,
        this.name
    );
  }
}
