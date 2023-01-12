package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.AbsoluteInterval;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Spans;

import java.util.Set;

public record SpansInterval(AbsoluteInterval interval) implements Expression<Spans> {

  @Override
  public Spans evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final Interval relativeInterval = interval.toRelative(results.planStart);
    return new Spans(Interval.intersect(bounds, relativeInterval));
  }

  @Override
  public void extractResources(final Set<String> names) {}

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(spans-interval %s)",
        prefix,
        this.interval
    );
  }
}
