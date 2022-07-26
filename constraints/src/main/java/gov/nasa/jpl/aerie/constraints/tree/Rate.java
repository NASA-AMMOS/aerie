package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record Rate(
    LinearProfileExpression profile) implements LinearProfileExpression {

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    return this.profile.evaluate(results, bounds, environment).rate();
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.profile.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "(rate-of %s)",
        this.profile.prettyPrint(prefix + "  ")
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final Rate o)) return false;

    return Objects.equals(this.profile, o.profile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.profile);
  }
}
