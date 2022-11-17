package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity;
import gov.nasa.jpl.aerie.constraints.time.Spans;

import java.util.Set;

public record SplitSpans(
    Expression<Spans> spansExpression, int numberOfSubIntervals,
    Inclusivity internalStartInclusivity,
    Inclusivity internalEndInclusivity) implements Expression<Spans> {

  @Override
  public Spans evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final var spans = this.spansExpression.evaluate(results, bounds, environment);
    return spans.split(this.numberOfSubIntervals, this.internalStartInclusivity, this.internalEndInclusivity);
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.spansExpression.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(split %s into %s)",
        prefix,
        this.spansExpression.prettyPrint(prefix + "  "),
        this.numberOfSubIntervals
    );
  }
}
