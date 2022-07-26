package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record Times(
    LinearProfileExpression profile,
    double multiplier) implements LinearProfileExpression {

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    return this.profile.evaluate(results, bounds, environment).times(this.multiplier);
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.profile.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(* %s %s)",
        prefix,
        this.profile.prettyPrint(prefix + "  "),
        String.format("\n%s  %s", prefix, this.multiplier)
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final Times o)) return false;

    return Objects.equals(this.profile, o.profile) &&
           Objects.equals(this.multiplier, o.multiplier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.profile, this.multiplier);
  }
}
