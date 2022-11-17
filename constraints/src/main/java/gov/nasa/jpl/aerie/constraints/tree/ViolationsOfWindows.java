package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.profile.Windows;
import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.List;
import java.util.Set;

public record ViolationsOfWindows(
    Expression<Windows> expression) implements Expression<List<Violation>> {

  @Override
  public List<Violation> evaluate(SimulationResults results, final Interval bounds, EvaluationEnvironment environment) {
    final var satisfiedWindows = (Windows) this.expression.evaluate(results, bounds, environment);
    return List.of(new Violation(satisfiedWindows.filterValues($ -> !$).iterableIntervals(bounds)));
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.expression.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return this.expression.prettyPrint(prefix);
  }
}
