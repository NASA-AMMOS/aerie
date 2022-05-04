package gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateQueryParam;

public class DurationExpressionState implements DurationExpression{

  final StateQueryParam state;

  public DurationExpressionState(StateQueryParam state){
    this.state = state;
  }

  @Override
  public Duration compute(final Window window, final SimulationResults simulationResults) {
    DurationValueMapper mapper = new DurationValueMapper();
    var resDes = mapper.deserializeValue(state.getValue(simulationResults, null, window));
    return resDes.getSuccessOrThrow();
  }
}
