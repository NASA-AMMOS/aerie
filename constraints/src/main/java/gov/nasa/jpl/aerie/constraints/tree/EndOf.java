package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;

public final class EndOf implements Expression<Windows> {
  private final String activityAlias;

  public EndOf(final String activityAlias) {
    this.activityAlias = activityAlias;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    final var activity = environment.get(this.activityAlias);
    return new Windows(Window.at(activity.window.end));
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(end-of %s)",
        prefix,
        this.activityAlias
    );
  }
}
