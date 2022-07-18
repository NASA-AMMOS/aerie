package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ForEachActivity implements Expression<List<Violation>> {
  public final String activityType;
  public final String alias;
  public final Expression<List<Violation>> expression;

  public ForEachActivity(
      final String activityType,
      final String alias,
      final Expression<List<Violation>> expression
  ) {
    this.activityType = activityType;
    this.alias = alias;
    this.expression = expression;
  }

  @Override
  public List<Violation> evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    final var violations = new ArrayList<Violation>();
    for (final var activity : results.activities) {
      if (activity.type.equals(this.activityType)) {
        final var newEnvironment = new HashMap<String, ActivityInstance>();
        newEnvironment.put(this.alias, activity);
        newEnvironment.putAll(environment);

        final var expressionViolations = this.expression.evaluate(results, bounds, newEnvironment);
        for (final var violation : expressionViolations) {
          if (!violation.violationWindows.isEmpty()) {
            final var newViolation = violation.clone();
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
    if (!(obj instanceof ForEachActivity)) return false;
    final var o = (ForEachActivity)obj;

    return Objects.equals(this.activityType, o.activityType) &&
           Objects.equals(this.alias, o.alias) &&
           Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityType, this.alias, this.expression);
  }
}
