package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public class DurationExpressionState implements DurationExpression{

  final StateQueryParam state;

  public DurationExpressionState(StateQueryParam state){
    this.state = state;
  }

  @Override
  public Duration compute(final Window window) {
    DurationValueMapper mapper = new DurationValueMapper();
    var resDes = mapper.deserializeValue(state.getValue(null, window));
    return resDes.getSuccessOrThrow();
  }
}
