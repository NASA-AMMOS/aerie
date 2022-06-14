package gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

public class TimeExpressionFromExpr extends TimeExpression {

  protected final TimeExpression expression;
  protected boolean fixed = true;
  protected final String name;

  public TimeExpressionFromExpr(final TimeExpression expression, final String name) {
    this.expression = expression;
    this.name = name;
  }

  @Override
  public Window computeTime(final SimulationResults simulationResults, final Plan plan, final Window interval) {
    Window rangeExpr = expression.computeTime(simulationResults, plan, interval);
    Window retRange = null;

    if (rangeExpr != null) {
      Duration resMin = rangeExpr.start;
      Duration resMax = rangeExpr.end;
      for (final var entry : this.operations) {
        resMin = TimeUtility.performOperation(entry.getKey(), resMin, entry.getValue());
        resMax = TimeUtility.performOperation(entry.getKey(), resMax, entry.getValue());
      }

      retRange = Window.between(resMin, resMax);

    }
    return retRange;
  }
}
