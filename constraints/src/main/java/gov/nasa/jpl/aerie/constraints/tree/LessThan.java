package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;
import java.util.Objects;

public final class LessThan implements Expression<Windows> {
  public final Expression<LinearProfile> left;
  public final Expression<LinearProfile> right;

  public LessThan(final Expression<LinearProfile> left, final Expression<LinearProfile> right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    LinearProfile leftProfile = this.left.evaluate(results, environment);
    LinearProfile rightProfile = this.right.evaluate(results, environment);

    return leftProfile.lessThan(rightProfile, results.bounds);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(< %s %s)",
        prefix,
        this.left.prettyPrint(prefix + "  "),
        this.right.prettyPrint(prefix + "  ")
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LessThan)) return false;
    final var o = (LessThan)obj;

    return Objects.equals(this.left, o.left) &&
           Objects.equals(this.right, o.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.left, this.right);
  }
}
