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

  public TimeExpressionConstant(Duration instant) {
    this.instant = instant;
  }

  @Override
  public Window computeTime(SimulationResults simulationResults, Plan plan, Window interval) {
    return Window.at(this.instant);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TimeExpressionConstant that = (TimeExpressionConstant) o;
    return Objects.equals(instant, that.instant) && Objects.equals(operations, that.operations);
  }
}
