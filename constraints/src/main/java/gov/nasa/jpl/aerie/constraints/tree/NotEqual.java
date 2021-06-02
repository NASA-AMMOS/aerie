package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.Profile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class NotEqual<P extends Profile<P>> implements Expression<Windows> {
  private final Expression<P> left;
  private final Expression<P> right;

  public NotEqual(final Expression<P> left, final Expression<P> right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    final var leftProfile = this.left.evaluate(results, environment);
    final var rightProfile = this.right.evaluate(results, environment);

    return leftProfile.notEqualTo(rightProfile, results.bounds);
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.left.extractResources(names);
    this.right.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(!= %s %s)",
        prefix,
        this.left.prettyPrint(prefix + "  "),
        this.right.prettyPrint(prefix + "  ")
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof NotEqual)) return false;
    final var o = (NotEqual<?>)obj;

    return Objects.equals(this.left, o.left) &&
           Objects.equals(this.right, o.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.left, this.right);
  }
}
