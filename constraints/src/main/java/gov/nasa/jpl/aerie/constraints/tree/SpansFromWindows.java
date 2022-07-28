package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record SpansFromWindows(Expression<Windows> expression) implements Expression<Spans> {

  @Override
  public Spans evaluate(SimulationResults results, final Window bounds, Map<String, ActivityInstance> environment) {
    final var windows = this.expression.evaluate(results, bounds, environment);
    return windows.intoSpans();
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.expression.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(spans-from %s)",
        prefix,
        this.expression.prettyPrint(prefix + "  ")
    );
  }
}
