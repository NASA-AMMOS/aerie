package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.Map;

public final class RealResource implements Expression<LinearProfile> {
  public final String name;

  public RealResource(final String name) {
    this.name = name;
  }

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return results.realProfiles.get(this.name);
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
