package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Spans;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public record ForEachActivitySpans(
    String activityType, String alias,
    Expression<Spans> expression) implements Expression<Spans> {

  @Override
  public Spans evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final var spans = new Spans();
    for (final var activity : results.activities) {
      if (activity.type.equals(this.activityType)) {
        final var newEnvironment = new EvaluationEnvironment(
            new HashMap<>(environment.activityInstances()),
            environment.realExternalProfiles(),
            environment.discreteExternalProfiles()
        );
        newEnvironment.activityInstances().put(this.alias, activity);

        final var expressionSpans = this.expression.evaluate(results, bounds, newEnvironment);
        spans.addAll(expressionSpans);
      }
    }
    return spans;
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.expression.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(for-each-activity %s %s %s)",
        prefix,
        this.activityType,
        this.alias,
        this.expression.prettyPrint(prefix + "  ")
    );
  }
}
