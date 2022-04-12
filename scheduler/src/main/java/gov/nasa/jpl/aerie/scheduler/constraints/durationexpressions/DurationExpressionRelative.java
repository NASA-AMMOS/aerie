package gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public class DurationExpressionRelative implements DurationExpression {


  final DurationAnchorEnum anchor;

  public DurationExpressionRelative(DurationAnchorEnum anchor){
    this.anchor = anchor;
  }


  @Override
  public Duration compute(final Window window) {
    if(anchor==DurationAnchorEnum.WindowDuration){
      return window.duration();
    }
    throw new IllegalArgumentException("Not implemented: Duration anchor different than WindowDuration");
  }
}
