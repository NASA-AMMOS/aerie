package gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.Map;
import java.util.Objects;

public class TimeExpressionConstant extends TimeExpression {

  protected final Duration instant;

  public TimeExpressionConstant(final Duration instant) {
    this.instant = instant;
  }

  @Override
  public Window computeTime(final SimulationResults simulationResults, final Plan plan, final Window interval) {
    return Window.at(this.instant);
  }
}
