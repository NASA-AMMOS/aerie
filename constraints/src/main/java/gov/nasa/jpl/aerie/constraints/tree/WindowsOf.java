package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record WindowsOf(
    Expression<List<Violation>> expression) implements WindowsExpression {

  @Override
  public Windows evaluate(SimulationResults results, final Window bounds, Map<String, ActivityInstance> environment) {
    final var ret = new Windows(bounds);
    final var unsatisfiedWindows = this.expression.evaluate(results, bounds, environment);
    for (var unsatisfiedWindow : unsatisfiedWindows) {
      ret.intersectWith(unsatisfiedWindow.violationWindows);
    }
    return ret;
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
    if (!(obj instanceof final WindowsOf o)) return false;

    return Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expression);
  }
}
