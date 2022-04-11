package gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions;

import gov.nasa.jpl.aerie.constraints.time.Window;
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
  public TimeExpressionLatching(TimeExpression expr1, TimeExpression expr2, TimeRangeExpression expr) {
    this.expr1 = expr1;
    this.expr2 = expr2;
    resetWindowsExpression = expr;
  }


  private final TimeExpression expr1;
  private final TimeExpression expr2;
  private final TimeRangeExpression resetWindowsExpression;


  @Override
  public Window computeTime(Plan plan, Window interval) {

    List<Window> resetPeriods = StreamSupport
        .stream(resetWindowsExpression.computeRange(plan, Windows.forever()).spliterator(), false)
        .collect(Collectors.toList());
    boolean first = true;
    for (var window : resetPeriods) {
      Window inter = Window.intersect(window,interval);
      if (inter != null) {
        if (first) {
          return expr1.computeTime(plan, interval);
        } else {
          return expr2.computeTime(plan, interval);
        }

      }
      first = false;
    }
    return null;
  }
}
