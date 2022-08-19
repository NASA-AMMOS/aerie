package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class StartOf implements Expression<Windows> {
  public final String activityAlias;

  public StartOf(final String activityAlias) {
    this.activityAlias = activityAlias;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Interval bounds, final Map<String, ActivityInstance> environment) {
    final var activity = environment.get(this.activityAlias);
    return new Windows(
        Pair.of(Interval.FOREVER, false),
        Pair.of(Interval.at(activity.interval.start), true)
    );
  }

  @Override
  public void extractResources(final Set<String> names) { }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(start-of %s)",
        prefix,
        this.activityAlias
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof StartOf)) return false;
    final var o = (StartOf)obj;

    return Objects.equals(this.activityAlias, o.activityAlias);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityAlias);
  }
}
