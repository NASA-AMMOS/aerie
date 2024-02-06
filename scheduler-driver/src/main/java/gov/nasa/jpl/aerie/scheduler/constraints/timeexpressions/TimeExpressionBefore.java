package gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.Set;

public class TimeExpressionBefore extends TimeExpression {

  protected final String name;
  protected final TimeExpression expr;

  public TimeExpressionBefore(final TimeExpression expr, final String name) {
    this.name = name;
    this.expr = expr;
  }

  @Override
  public Interval computeTime(final SimulationResults simulationResults, final Plan plan, final Interval interval) {
    final var origin = expr.computeTime(simulationResults, plan, interval);
    assert(origin.isSingleton());
    final var from = origin.start;

    Duration res = from;
    for (final var entry : this.operations) {
      res = TimeUtility.performOperation(entry.getKey(), res, entry.getValue());
    }

    return res.compareTo(from) > 0 ? // If we want a range of possibles
        Interval.between(from, res) :
        Interval.between(res, from);
  }

  @Override
  public void extractResources(final Set<String> names) {
    expr.extractResources(names);
  }
}
