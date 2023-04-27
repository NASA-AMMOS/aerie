package gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public class DurationExpressionDur implements DurationExpression {

  final Duration dur;

  public DurationExpressionDur(Duration dur) {
    this.dur = dur;
  }

  @Override
  public Duration compute(final Interval interval, final SimulationResults simulationResults) {
    return dur;
  }
}
