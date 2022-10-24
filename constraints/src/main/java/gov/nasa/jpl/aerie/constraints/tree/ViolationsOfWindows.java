package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ViolationsOfWindows implements Expression<List<Violation>> {
  public final Expression<Windows> expression;

  public ViolationsOfWindows(Expression<Windows> expression) {
    this.expression = expression;
  }

  @Override
  public List<Violation> evaluate(SimulationResults results, final Interval bounds, EvaluationEnvironment environment) {
    final var satisfiedWindows = this.expression.evaluate(results, bounds, environment);
    return List.of(new Violation(satisfiedWindows.not().select(bounds)));
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
    if (!(obj instanceof ViolationsOfWindows)) return false;
    final var o = (ViolationsOfWindows)obj;

    return Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expression);
  }
}
