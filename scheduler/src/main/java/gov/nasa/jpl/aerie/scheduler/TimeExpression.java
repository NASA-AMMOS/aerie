package gov.nasa.jpl.aerie.scheduler;

import java.util.LinkedHashMap;

/**
 * class allowing to define dynamic expressions of timepoints, relative to time anchors
 */
public abstract class TimeExpression {

  /**
   * A TimeExpression must implement this method
   *
   * @param plan the current plan
   * @param interval the range on which the relative time expression must be computed
   * @return a range of valid times satisfying the expression
   */
  public abstract Range<Time> computeTime(Plan plan, Range<Time> interval);


  protected LinkedHashMap<Time.Operator, Duration> operations = new LinkedHashMap<Time.Operator, Duration>();


  public static TimeExpression fromAnchor(TimeAnchor anchor) {
    return new TimeExpressionRelative(anchor, true, DEF_NAME);
  }

  /**
   * Builder allowing to create Latching time expression
   */
  public static class LatchingBuilder {

    TimeExpression expr1;
    TimeExpression expr2;
    TimeRangeExpression expr;

    LatchingBuilder withinEach(TimeRangeExpression expr) {
      this.expr = expr;
      return this;
    }

    LatchingBuilder first(TimeExpression filter) {
      expr1 = filter;
      return this;
    }

    LatchingBuilder andThen(TimeExpression filter) {
      expr2 = filter;
      return this;
    }

    TimeExpression build() {
      TimeExpression exprLatch = new TimeExpressionLatching(expr1, expr2, expr);
      return exprLatch;
    }


  }

  public static final TimeExpression atStart() {
    return new TimeExpressionRelative(TimeAnchor.START, true, DEF_NAME);
  }

  public static final TimeExpression offsetByAfterStart(Duration dur) {
    TimeExpression te = new TimeExpressionRelative(TimeAnchor.START, true, DEF_NAME);
    te.operations.put(Time.Operator.PLUS, dur);
    return te;
  }


  public static final TimeExpression offsetByBeforeStart(Duration dur) {
    TimeExpression te = new TimeExpressionRelative(TimeAnchor.START, true, DEF_NAME);
    te.operations.put(Time.Operator.MINUS, dur);
    return te;
  }


  public static final TimeExpression offsetByAfterEnd(Duration dur) {
    TimeExpression te = new TimeExpressionRelative(TimeAnchor.END, true, DEF_NAME);
    te.operations.put(Time.Operator.PLUS, dur);
    return te;
  }


  public static final TimeExpression offsetByBeforeEnd(Duration dur) {
    TimeExpression te = new TimeExpressionRelative(TimeAnchor.END, true, DEF_NAME);
    te.operations.put(Time.Operator.MINUS, dur);
    return te;
  }

  public static final TimeExpression beforeEnd() {
    TimeExpression te = new TimeExpressionRelative(TimeAnchor.END, false, DEF_NAME);
    te.operations.put(Time.Operator.MINUS, Duration.ofMaxDur());
    return te;
  }

  public static final TimeExpression beforeStart() {
    TimeExpression te = new TimeExpressionRelative(TimeAnchor.START, false, DEF_NAME);
    te.operations.put(Time.Operator.MINUS, Duration.ofMaxDur());
    return te;
  }

  public static final TimeExpression afterEnd() {
    TimeExpression te = new TimeExpressionRelative(TimeAnchor.END, false, DEF_NAME);
    te.operations.put(Time.Operator.PLUS, Duration.ofMaxDur());
    return te;
  }

  public static TimeExpression afterStart() {
    TimeExpression te = new TimeExpressionRelative(TimeAnchor.START, false, DEF_NAME);
    te.operations.put(Time.Operator.PLUS, Duration.ofMaxDur());
    return te;
  }

  public static final TimeExpression withinAfterStart(Duration dur) {
    TimeExpression te = new TimeExpressionRelative(TimeAnchor.START, false, DEF_NAME);
    te.operations.put(Time.Operator.PLUS, dur);
    return te;
  }


  public static String DEF_NAME = "NO_NAME_TIME_EXPR";

  public static class Builder {
    private boolean interval = false;
    private String name = DEF_NAME;

    public Builder getThis() {
      return this;
    }


    public Builder name(String name) {
      this.name = name;
      return getThis();
    }

    public Builder from(TimeExpression otherExpr) {
      fromExpression = otherExpr;
      return getThis();
    }

    TimeExpression fromExpression;

    public Builder from(TimeAnchor anchor) {
      fromAnchor = anchor;
      return getThis();
    }

    TimeAnchor fromAnchor;

    public Builder minus(Duration dur) {
      operations.put(Time.Operator.MINUS, dur);
      return getThis();
    }

    public Builder interval() {
      this.interval = true;
      return getThis();
    }

    public Builder plus(Duration dur) {
      operations.put(Time.Operator.PLUS, dur);
      return getThis();
    }

    protected LinkedHashMap<Time.Operator, Duration> operations = new LinkedHashMap<Time.Operator, Duration>();

    public TimeExpression build() {
      if (fromExpression != null) {
        var expr = new TimeExpressionFromExpr(fromExpression, name);
        expr.operations = operations;
        return expr;
      } else if (fromAnchor != null) {
        var expr = new TimeExpressionRelative(fromAnchor, interval, name);
        expr.operations = operations;
        return expr;
      } else {
        throw new RuntimeException("Time expression must either be another time expression or a time anchor");
      }

    }

  }

}
