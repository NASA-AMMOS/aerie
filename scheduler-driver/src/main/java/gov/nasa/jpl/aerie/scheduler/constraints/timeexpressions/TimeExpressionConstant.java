package gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.Set;

public class TimeExpressionConstant extends TimeExpression {

  protected final Duration instant;

  public TimeExpressionConstant(final Duration instant) {
    this.instant = instant;
  }

  @Override
  public Interval computeTime(final SimulationResults simulationResults, final Plan plan, final Interval interval) {
    return Interval.at(this.instant);
  }

  @Override
  public void extractResources(final Set<String> names) { }
}
