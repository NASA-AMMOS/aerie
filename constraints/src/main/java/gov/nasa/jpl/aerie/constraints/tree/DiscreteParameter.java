package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DiscreteParameter implements Expression<DiscreteProfile> {
  public final String activityAlias;
  public final String parameterName;

  public DiscreteParameter(final String activityAlias, final String parameterName) {
    this.activityAlias = activityAlias;
    this.parameterName = parameterName;
  }

  @Override
  public DiscreteProfile evaluate(final SimulationResults results, final Interval bounds, final Map<String, ActivityInstance> environment) {
    final var activity = environment.get(this.activityAlias);
    return new DiscreteProfile(
        List.of(
            new DiscreteProfilePiece(bounds, activity.parameters.get(this.parameterName))));
  }

  @Override
  public void extractResources(final Set<String> names) { }

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
    if (!(obj instanceof DiscreteParameter)) return false;
    final var o = (DiscreteParameter)obj;

    return Objects.equals(this.activityAlias, o.activityAlias) &&
           Objects.equals(this.parameterName, o.parameterName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityAlias, this.parameterName);
  }
}
