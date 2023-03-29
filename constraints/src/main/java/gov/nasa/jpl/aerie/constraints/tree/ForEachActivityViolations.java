package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ForEachActivityViolations implements Expression<List<Violation>> {
  public final String activityType;
  public final String alias;
  public final Expression<List<Violation>> expression;

  public ForEachActivityViolations(
      final String activityType,
      final String alias,
      final Expression<List<Violation>> expression
  ) {
    this.activityType = activityType;
    this.alias = alias;
    this.expression = expression;
  }

  @Override
  public List<Violation> evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final var violations = new ArrayList<Violation>();
    for (final var activity : results.activities) {
      if (activity.type.equals(this.activityType)) {
        final var newEnvironment = new EvaluationEnvironment(
            new HashMap<>(environment.activityInstances()),
            environment.spansInstances(),
            environment.intervals(),
            environment.realExternalProfiles(),
            environment.discreteExternalProfiles()
        );
        newEnvironment.activityInstances().put(this.alias, activity);

        final var expressionViolations = this.expression.evaluate(results, bounds, newEnvironment);
        for (final var violation : expressionViolations) {
          if (!violation.violationWindows.isEmpty()) {
            final var newViolation = new Violation(violation);
            newViolation.addActivityId(activity.id);
            violations.add(newViolation);
          }
        }
      }
    }
    return violations;
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
    if (!(obj instanceof ForEachActivityViolations)) return false;
    final var o = (ForEachActivityViolations)obj;

    return Objects.equals(this.activityType, o.activityType) &&
           Objects.equals(this.alias, o.alias) &&
           Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityType, this.alias, this.expression);
  }
}
