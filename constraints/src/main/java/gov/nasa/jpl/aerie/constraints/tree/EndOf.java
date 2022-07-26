package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EndOf(String activityAlias) implements WindowsExpression {

  @Override
  public Windows evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    final var activity = environment.get(this.activityAlias);
    return new Windows(Window.at(activity.window.end));
  }

  @Override
  public void extractResources(final Set<String> names) {
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(end-of %s)",
        prefix,
        this.activityAlias
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final EndOf o)) return false;

    return Objects.equals(this.activityAlias, o.activityAlias);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityAlias);
  }
}
