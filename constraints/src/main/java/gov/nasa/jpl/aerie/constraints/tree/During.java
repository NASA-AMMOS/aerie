package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;

public final class During implements Expression<Windows> {
  private final String activityAlias;

  public During(final String activityAlias) {
    this.activityAlias = activityAlias;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    final var activity = environment.get(this.activityAlias);
    return new Windows(activity.window);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(during %s)",
        prefix,
        this.activityAlias
    );
  }
}
