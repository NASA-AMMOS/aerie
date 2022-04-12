package gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.Map;
import java.util.Objects;

public class TimeExpressionBefore extends TimeExpression {

  protected final String name;
  protected final TimeExpression expr;

  public TimeExpressionBefore(TimeExpression expr, String name) {
    this.name = name;
    this.expr = expr;
  }

  @Override
  public Window computeTime(Plan plan, Window interval) {
    var origin =     expr.computeTime(plan, interval);
    assert(origin.isSingleton());
    Duration from = origin.start;

    Duration res = from;
    for (Map.Entry<TimeUtility.Operator, Duration> entry : this.operations.entrySet()) {
      res = TimeUtility.performOperation(entry.getKey(), res, entry.getValue());
    }

    Window retRange;

  //if we want an range of possibles
    if (res.compareTo(from) > 0) {
      retRange = Window.between(from, res);
    } else {
      retRange = Window.between(res, from);
    }
    return retRange;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TimeExpressionBefore that = (TimeExpressionBefore) o;
    return Objects.equals(expr, that.expr) && Objects.equals(operations, that.operations);
  }
}
