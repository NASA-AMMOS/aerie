package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.IntervalContainer;

import java.util.Objects;
import java.util.Set;

public record Ends<I extends IntervalContainer<I>>(
    Expression<I> expression) implements Expression<I> {

  @Override
  public I evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final var expression = this.expression.evaluate(results, bounds, environment);
    return expression.ends();
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.expression.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(ends-of %s)",
        prefix,
        this.expression.prettyPrint(prefix + "  ")
    );
  }
}
