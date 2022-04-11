package gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public class DurationExpressionMinus implements DurationExpression{

  final DurationExpression expr1;
  final DurationExpression expr2;

  public DurationExpressionMinus(DurationExpression expr1, DurationExpression expr2){
    this.expr1 = expr1;
    this.expr2 = expr2;

  }

  @Override
  public Duration compute(final Window window) {
    return expr1.compute(window).minus(expr2.compute(window));
  }
}
