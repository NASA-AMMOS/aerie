package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.IntervalContainer;
import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Split<I extends IntervalContainer<I>> implements Expression<I> {
  public final Expression<I> intervals;
  public final int numberOfSubIntervals;

  public Split(final Expression<I> intervals, final int numberOfSubIntervals) {
    this.intervals = intervals;
    this.numberOfSubIntervals = numberOfSubIntervals;
  }

  @Override
  public I evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    final var intervals = this.intervals.evaluate(results, bounds, environment);
    return intervals.split(this.numberOfSubIntervals);
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.intervals.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(split %s into %s)",
        prefix,
        this.intervals.prettyPrint(prefix + "  "),
        this.numberOfSubIntervals
    );
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Split<?> split = (Split<?>) o;
    return numberOfSubIntervals == split.numberOfSubIntervals && Objects.equals(intervals, split.intervals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(intervals, numberOfSubIntervals);
  }
}
