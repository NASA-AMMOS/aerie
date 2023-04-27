package gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public class DurationExpressionRelative implements DurationExpression {

  final DurationAnchorEnum anchor;

  public DurationExpressionRelative(DurationAnchorEnum anchor) {
    this.anchor = anchor;
  }

  @Override
  public Duration compute(final Interval interval, final SimulationResults simulationResults) {
    if (anchor == DurationAnchorEnum.WindowDuration) {
      return interval.duration();
    }
    throw new IllegalArgumentException(
        "Not implemented: Duration anchor different than WindowDuration");
  }
}
