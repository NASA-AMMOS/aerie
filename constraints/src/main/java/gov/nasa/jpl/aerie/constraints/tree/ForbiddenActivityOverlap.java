package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ForbiddenActivityOverlap implements Expression<List<Violation>> {
  public final String activityType1;
  public final String activityType2;

  public ForbiddenActivityOverlap(final String activityType1, final String activityType2) {
    this.activityType1 = activityType1;
    this.activityType2 = activityType2;
  }

  @Override
  public List<Violation> evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    final var expansion =
        new ForEachActivity(
            this.activityType1,
            "act1",
            new ForEachActivity(
                this.activityType2,
                "act2",
                new ViolationsOf(
                    new Not(new And(new During("act1"), new During("act2"))))));

    return expansion.evaluate(results, bounds, environment);
  }

  @Override
  public void extractResources(final Set<String> names) {
    final var expansion =
        new ForEachActivity(
            this.activityType1,
            "act1",
            new ForEachActivity(
                this.activityType2,
                "act2",
                new ViolationsOf(
                    new Not(new And(new During("act1"), new During("act2"))))));

    expansion.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return "\n%s(forbidden-activity-overlap %s %s)"
        .formatted(prefix, this.activityType1, this.activityType2);
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof ForbiddenActivityOverlap o)) return false;

    return Objects.equals(this.activityType1, o.activityType1) &&
           Objects.equals(this.activityType2, o.activityType2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityType1, this.activityType2);
  }
}
