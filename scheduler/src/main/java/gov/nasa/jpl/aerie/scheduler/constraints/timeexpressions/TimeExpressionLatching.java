package gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A latching time expression is computed with one expression if on the first element of a larger sequence and with
 * another time expression if on the
 * remaining element of the larger sequence
 */
public class TimeExpressionLatching extends TimeExpression {
  public TimeExpressionLatching(final TimeExpression expr1, final TimeExpression expr2, final TimeRangeExpression expr) {
    this.expr1 = expr1;
    this.expr2 = expr2;
    resetWindowsExpression = expr;
  }


  private final TimeExpression expr1;
  private final TimeExpression expr2;
  private final TimeRangeExpression resetWindowsExpression;


  @Override
  public Interval computeTime(final SimulationResults simulationResults, final Plan plan, final Interval interval) {

    List<Interval> resetPeriods = StreamSupport
        .stream(resetWindowsExpression.computeRange(simulationResults, plan, new Windows(Interval.FOREVER, true)).spliterateTrue(), false)
        .toList();
    boolean first = true;
    for (var window : resetPeriods) {
      Interval inter = Interval.intersect(window,interval);
      if (inter != null) {
        if (first) {
          return expr1.computeTime(simulationResults, plan, interval);
        } else {
          return expr2.computeTime(simulationResults, plan, interval);
        }

      }
      first = false;
    }
    return null;
  }
}
