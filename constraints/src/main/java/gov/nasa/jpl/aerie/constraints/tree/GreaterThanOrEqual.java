package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;

public final class GreaterThanOrEqual implements Expression<Windows> {
  public final Expression<LinearProfile> left;
  public final Expression<LinearProfile> right;

  public GreaterThanOrEqual(final Expression<LinearProfile> left, final Expression<LinearProfile> right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    final var leftProfile = this.left.evaluate(results, environment);
    final var rightProfile = this.right.evaluate(results, environment);

    return leftProfile.greaterThanOrEqualTo(rightProfile, results.bounds);
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
}
