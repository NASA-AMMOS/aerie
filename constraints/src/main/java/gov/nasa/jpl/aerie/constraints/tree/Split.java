package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Split implements Expression<Windows> {
  public final Expression<Windows> windows;
  public final int numberOfSubWindows;

  public Split(final Expression<Windows> windows, final int numberOfSubWindows) {
    this.windows = windows;
    this.numberOfSubWindows = numberOfSubWindows;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    final var windows = this.windows.evaluate(results, bounds, environment);
    return windows.split(this.numberOfSubWindows);
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.windows.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(split %s into %s)",
        prefix,
        this.windows.prettyPrint(prefix + "  "),
        this.numberOfSubWindows
    );
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Split split = (Split) o;
    return numberOfSubWindows == split.numberOfSubWindows && Objects.equals(windows, split.windows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(windows, numberOfSubWindows);
  }
}
