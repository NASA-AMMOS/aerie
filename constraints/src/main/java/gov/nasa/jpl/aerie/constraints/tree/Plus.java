package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.Map;
import java.util.Objects;

public final class Plus implements Expression<LinearProfile> {
  private final Expression<LinearProfile> left;
  private final Expression<LinearProfile> right;

  public Plus(final Expression<LinearProfile> left, final Expression<LinearProfile> right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return left.evaluate(results, environment)
               .plus(right.evaluate(results, environment));
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(+ %s %s)",
        prefix,
        this.left.prettyPrint(prefix + "  "),
        this.right.prettyPrint(prefix + "  ")
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Plus)) return false;
    final var o = (Plus)obj;

    return Objects.equals(this.left, o.left) &&
           Objects.equals(this.right, o.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.left, this.right);
  }
}
