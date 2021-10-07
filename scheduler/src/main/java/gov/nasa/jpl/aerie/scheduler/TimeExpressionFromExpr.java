package gov.nasa.jpl.aerie.scheduler;

import java.util.Map;

public class TimeExpressionFromExpr extends TimeExpression {

  protected TimeExpression expression;
  protected boolean fixed = true;
  protected String name;

  public TimeExpressionFromExpr(TimeExpression expression, String name) {
    this.expression = expression;
    this.name = name;
  }

  @Override
  public Range<Time> computeTime(Plan plan, Range<Time> interval) {

    Range<Time> rangeExpr = expression.computeTime(plan, interval);
    Range<Time> retRange = null;

    if (rangeExpr != null) {


      Time resMin = rangeExpr.getMinimum();
      Time resMax = rangeExpr.getMaximum();
      for (Map.Entry<Time.Operator, Duration> entry : this.operations.entrySet()) {
        resMin = Time.performOperation(entry.getKey(), resMin, entry.getValue());
        resMax = Time.performOperation(entry.getKey(), resMax, entry.getValue());

      }

      retRange = new Range<Time>(resMin, resMax);

    }
    return retRange;
  }
}
