package gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public interface DurationExpression {

  enum DurationAnchorEnum {
    WindowDuration
  }

  Duration compute(Window window, final SimulationResults simulationResults);

  default DurationExpression minus(DurationExpression other){
    return new DurationExpressionMinus(this, other);
  }

}
