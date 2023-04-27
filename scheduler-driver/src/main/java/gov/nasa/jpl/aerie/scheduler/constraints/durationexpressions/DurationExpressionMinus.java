package gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public class DurationExpressionMinus implements DurationExpression {

  final DurationExpression expr1;
  final DurationExpression expr2;

  public DurationExpressionMinus(DurationExpression expr1, DurationExpression expr2) {
    this.expr1 = expr1;
    this.expr2 = expr2;
  }

  @Override
  public Duration compute(final Interval interval, final SimulationResults simulationResults) {
    return expr1
        .compute(interval, simulationResults)
        .minus(expr2.compute(interval, simulationResults));
  }
}
