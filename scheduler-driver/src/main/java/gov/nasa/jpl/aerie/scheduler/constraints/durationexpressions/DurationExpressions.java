package gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public class DurationExpressions {

  public static DurationExpression constant(Duration dur) {
    return new DurationExpressionDur(dur);
  }

  public static DurationExpression windowDuration() {
    return new DurationExpressionRelative(DurationExpression.DurationAnchorEnum.WindowDuration);
  }

  public static DurationExpression max(DurationExpression... expr) {
    return new DurationExpressionMax(expr);
  }
}
