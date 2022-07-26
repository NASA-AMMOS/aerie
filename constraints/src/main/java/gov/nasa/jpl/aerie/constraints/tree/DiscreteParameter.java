package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record DiscreteParameter(
    String activityAlias,
    String parameterName) implements DiscreteProfileExpression {

  @Override
  public DiscreteProfile evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    final var activity = environment.get(this.activityAlias);
    return new DiscreteProfile(
        List.of(
            new DiscreteProfilePiece(bounds, activity.parameters.get(this.parameterName))));
  }

  @Override
  public void extractResources(final Set<String> names) {
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

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final DiscreteParameter o)) return false;

    return Objects.equals(this.activityAlias, o.activityAlias) &&
           Objects.equals(this.parameterName, o.parameterName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityAlias, this.parameterName);
  }
}
