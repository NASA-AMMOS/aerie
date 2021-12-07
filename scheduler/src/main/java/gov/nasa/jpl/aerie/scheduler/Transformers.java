package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.ArrayList;
import java.util.List;

public class Transformers {

  public static TimeWindowsTransformer beforeEach() {
    return new TransformerBeforeEach(Duration.ZERO);
  }


  public static TimeWindowsTransformer afterEach(Duration dur) {
    return new TransformerAfterEach(dur);
  }

  public static TimeWindowsTransformer beforeEach(Duration dur) {
    return new TransformerBeforeEach(dur);
  }

  public static class EnveloppeBuilder {

    List<TimeRangeExpression> insideExprs = new ArrayList<TimeRangeExpression>();
    TimeRangeExpression resetExpr;

    public EnveloppeBuilder withinEach(TimeRangeExpression expr) {
      this.resetExpr = expr;
      return this;
    }

    public EnveloppeBuilder when(StateConstraintExpression expr) {
      insideExprs.add(new TimeRangeExpression.Builder().from(expr).build());
      return this;
    }


    public EnveloppeBuilder when(ActivityExpression expr) {
      insideExprs.add(new TimeRangeExpression.Builder().from(expr).build());
      return this;
    }

    public EnveloppeBuilder when(TimeRangeExpression expr) {
      insideExprs.add(expr);
      return this;
    }

    public TimeWindowsTransformer build() {
      TimeWindowsTransformer filter = new TransformWithReset(resetExpr, new TransformerEnveloppe(insideExprs));
      return filter;
    }


  }


}
