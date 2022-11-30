package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Spans;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public final class ForEachActivitySpans implements Expression<Spans> {
  public final String activityType;
  public final String alias;
  public final Expression<Spans> expression;

  public ForEachActivitySpans(
      final String activityType,
      final String alias,
      final Expression<Spans> expression
  ) {
    this.activityType = activityType;
    this.alias = alias;
    this.expression = expression;
  }

  @Override
  public Spans evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final var spans = new Spans();
    for (final var activity : results.activities) {
      if (activity.type.equals(this.activityType)) {
        final var newEnvironment = new EvaluationEnvironment(
            new HashMap<>(environment.activityInstances()),
            environment.spansInstances(),
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

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final ForEachActivitySpans o)) return false;

    return Objects.equals(this.activityType, o.activityType) &&
           Objects.equals(this.alias, o.alias) &&
           Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityType, this.alias, this.expression);
  }
}
