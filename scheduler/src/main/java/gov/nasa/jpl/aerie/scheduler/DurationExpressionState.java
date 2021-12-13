package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public class DurationExpressionState implements DurationExpression{

  StateQueryParam<Duration> state;

public DurationExpressionState(StateQueryParam<Duration> state){
  this.state = state;
}

  @Override
  public Duration compute(final Window window) {
    return state.getValue(null, window);
  }
}
