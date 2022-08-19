package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WindowsOf implements Expression<Windows> {
  public final Expression<List<Violation>> expression;

  public WindowsOf(Expression<List<Violation>> expression) {
    this.expression = expression;
  }

  @Override
  public Windows evaluate(SimulationResults results, final Interval bounds, Map<String, ActivityInstance> environment) {
    var ret = new Windows(bounds, false);
    final var unsatisfiedWindows = this.expression.evaluate(results, bounds, environment);
    for(var unsatisfiedWindow : unsatisfiedWindows){
      ret.setAllTrue(unsatisfiedWindow.violationWindows);
    }
    return ret.not();
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
    if (!(obj instanceof WindowsOf)) return false;
    final var o = (WindowsOf)obj;

    return Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expression);
  }
}
