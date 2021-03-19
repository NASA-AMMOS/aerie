package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;

public final class StartOf implements Expression<Windows> {
  private final String activityAlias;

  public StartOf(final String activityAlias) {
    this.activityAlias = activityAlias;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    final var activity = environment.get(this.activityAlias);
    return new Windows(Window.at(activity.window.start));
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(start-of %s)",
        prefix,
        this.activityAlias
    );
  }
}
