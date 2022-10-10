package gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateQueryParam;

public class DurationExpressionState implements DurationExpression {

  final StateQueryParam state;

  public DurationExpressionState(StateQueryParam state){
    this.state = state;
  }

  @Override
  public Duration compute(final Interval interval, final SimulationResults simulationResults) {
    return Duration.of(state.getValue(simulationResults, null, interval).asInt().orElseThrow(), Duration.MICROSECONDS);
  }
}
