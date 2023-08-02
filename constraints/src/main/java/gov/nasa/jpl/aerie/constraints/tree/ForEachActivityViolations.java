package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.ConstraintResult;
import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.HashMap;
import java.util.Set;

public record ForEachActivityViolations(
    String activityType, String alias,
    Expression<ConstraintResult> expression) implements Expression<ConstraintResult> {

  @Override
  public ConstraintResult evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    var violations = new ConstraintResult();
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

        final var newViolations = this.expression.evaluate(results, bounds, newEnvironment);
        for (final var violation: newViolations.violations) {
          violation.addActivityId(activity.id);
        }
        violations = ConstraintResult.merge(violations, newViolations);
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
}
