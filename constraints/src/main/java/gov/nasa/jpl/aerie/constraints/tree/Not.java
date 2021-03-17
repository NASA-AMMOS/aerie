package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;

public final class Not implements Expression<Windows> {
  public final Expression<Windows> expression;

  public Not(final Expression<Windows> expression) {
    this.expression = expression;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return Windows.minus(
        new Windows(results.bounds),
        this.expression.evaluate(results, environment));
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(not %s)",
        prefix,
        this.expression.prettyPrint(prefix + "  ")
    );
  }
}
