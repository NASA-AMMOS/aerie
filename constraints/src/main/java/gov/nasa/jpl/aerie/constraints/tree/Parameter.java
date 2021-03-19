package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.List;
import java.util.Map;

public final class Parameter implements Expression<DiscreteProfile> {
  private final String activityAlias;
  private final String parameterName;

  public Parameter(final String activityAlias, final String parameterName) {
    this.activityAlias = activityAlias;
    this.parameterName = parameterName;
  }

  @Override
  public DiscreteProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    final var activity = environment.get(this.activityAlias);
    return new DiscreteProfile(
        List.of(
            new DiscreteProfilePiece(results.bounds, activity.parameters.get(this.parameterName))));
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(parameter %s %s)",
        prefix,
        this.activityAlias,
        this.parameterName
    );
  }
}
