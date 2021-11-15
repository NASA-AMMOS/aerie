package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

public class Transformers {

  public static TimeWindowsTransformer beforeEach() {
    return new TransformerBeforeEach(Duration.ofZero());
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

    EnveloppeBuilder withinEach(TimeRangeExpression expr) {
      this.resetExpr = expr;
      return this;
    }

    EnveloppeBuilder when(StateConstraintExpression expr) {
      insideExprs.add(new TimeRangeExpression.Builder().from(expr).build());
      return this;
    }


    EnveloppeBuilder when(ActivityExpression expr) {
      insideExprs.add(new TimeRangeExpression.Builder().from(expr).build());
      return this;
    }

    EnveloppeBuilder when(TimeRangeExpression expr) {
      insideExprs.add(expr);
      return this;
    }

    TimeWindowsTransformer build() {
      TimeWindowsTransformer filter = new TransformWithReset(resetExpr, new TransformerEnveloppe(insideExprs));
      return filter;
    }


  }


}
