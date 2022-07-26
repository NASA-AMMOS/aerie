package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record GreaterThanOrEqual(
    Expression<LinearProfile> left,
    Expression<LinearProfile> right) implements WindowsExpression {

  @Override
  public Windows evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    final var leftProfile = this.left.evaluate(results, bounds, environment);
    final var rightProfile = this.right.evaluate(results, bounds, environment);

    return leftProfile.greaterThanOrEqualTo(rightProfile, bounds);
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.left.extractResources(names);
    this.right.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(>= %s %s)",
        prefix,
        this.left.prettyPrint(prefix + "  "),
        this.right.prettyPrint(prefix + "  ")
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final GreaterThanOrEqual o)) return false;

    return Objects.equals(this.left, o.left) &&
           Objects.equals(this.right, o.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.left, this.right);
  }
}
