package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Map;

public class TimeExpressionBefore extends TimeExpression {

  protected String name;
  protected TimeExpression expr;

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
    for (Map.Entry<Time.Operator, Duration> entry : this.operations.entrySet()) {
      res = Time.performOperation(entry.getKey(), res, entry.getValue());
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
}
