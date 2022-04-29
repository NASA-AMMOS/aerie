package gov.nasa.jpl.aerie.constraints.tree;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class IfThen implements Expression<Windows> {
  public final Expression<Windows> condition;
  public final Expression<Windows> expression;

  public IfThen(final Expression<Windows> condition, final Expression<Windows> expression) {
    this.condition = condition;
    this.expression = expression;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    return new Or(new Not(this.condition), this.expression).evaluate(results, bounds, environment);
  }

  @Override
  public void extractResources(final Set<String> names) {
    new Or(new Not(this.condition), this.expression).extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return "\n%s(if-then %s %s)".formatted(
        prefix,
        this.condition.prettyPrint(prefix + "  "),
        this.expression.prettyPrint(prefix + "  "));
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof IfThen o)) return false;

    return Objects.equals(this.condition, o.condition) &&
           Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.condition, this.expression);
  }
}
