package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ViolationsOf implements Expression<List<Violation>> {
  public final Expression<Windows> expression;

  public ViolationsOf(Expression<Windows> expression) {
    this.expression = expression;
  }

  @Override
  public List<Violation> evaluate(SimulationResults results, final Window bounds, Map<String, ActivityInstance> environment) {
    final var boundsW = new Windows(bounds);
    final var satisfiedWindows = this.expression.evaluate(results, bounds, environment);
    return List.of(new Violation(Windows.minus(boundsW, satisfiedWindows)));
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.expression.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return this.expression.prettyPrint(prefix);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ViolationsOf)) return false;
    final var o = (ViolationsOf)obj;

    return Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expression);
  }
}
