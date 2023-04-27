package gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import java.util.List;

public class DurationExpressionMax implements DurationExpression {

  final List<DurationExpression> exprs;

  public DurationExpressionMax(DurationExpression... exprs) {
    this.exprs = List.of(exprs);
  }

  @Override
  public Duration compute(final Interval interval, final SimulationResults simulationResults) {
    var computed = new Duration[exprs.size()];
    int i = 0;
    for (var expr : exprs) {
      computed[i++] = expr.compute(interval, simulationResults);
    }

    return Duration.max(computed);
  }
}
