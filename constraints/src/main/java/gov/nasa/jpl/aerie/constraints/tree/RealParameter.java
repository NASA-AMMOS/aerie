package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RealParameter implements Expression<LinearProfile> {
  public final String activityAlias;
  public final String parameterName;

  public RealParameter(final String activityAlias, final String parameterName) {
    this.activityAlias = activityAlias;
    this.parameterName = parameterName;
  }

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    final var activity = environment.get(this.activityAlias);
    final var parameter = activity.parameters.get(this.parameterName);
    final var value = parameter.asReal().orElseThrow(
        () -> new InputMismatchException(
            String.format("Activity parameter \"%s\" with value %s cannot be interpreted as real",
                          this.parameterName,
                          activity.parameters.get(parameterName).toString())));

    return new LinearProfile(
        List.of(
            new LinearProfilePiece(results.bounds, value, 0)));
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
    if (!(obj instanceof RealParameter)) return false;
    final var o = (RealParameter)obj;

    return Objects.equals(this.activityAlias, o.activityAlias) &&
           Objects.equals(this.parameterName, o.parameterName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityAlias, this.parameterName);
  }
}
