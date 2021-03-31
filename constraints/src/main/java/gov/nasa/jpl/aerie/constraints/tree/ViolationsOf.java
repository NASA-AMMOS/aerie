package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.List;
import java.util.Map;

public final class ViolationsOf implements Expression<List<Violation>> {
  private final Expression<Windows> expression;

  public ViolationsOf(Expression<Windows> expression) {
    this.expression = expression;
  }

  @Override
  public List<Violation> evaluate(SimulationResults results, Map<String, ActivityInstance> environment) {
    final var bounds = new Windows(results.bounds);
    final var satisfiedWindows = this.expression.evaluate(results, environment);
    return List.of(new Violation(Windows.minus(bounds, satisfiedWindows)));
  }

  public List<Violation> evaluate(SimulationResults results) {
    return evaluate(results, Map.of());
  }

  @Override
  public String prettyPrint(final String prefix) {
    return this.expression.prettyPrint(prefix);
  }
}
