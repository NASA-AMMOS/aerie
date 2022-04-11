package gov.nasa.jpl.aerie.scheduler.constraints.transformers;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.List;

public class TransformerEnveloppe implements TimeWindowsTransformer {

  private final List<TimeRangeExpression> insideExprs;


  public TransformerEnveloppe(List<TimeRangeExpression> insideExprs) {
    this.insideExprs = insideExprs;
  }

  @Override
  public Windows transformWindows(Plan plan, Windows windowsToTransform) {

    Windows ret = new Windows();
    if(!windowsToTransform.isEmpty()) {
      Duration min = windowsToTransform.maxTimePoint().get(), max = windowsToTransform.minTimePoint().get();
      boolean atLeastOne = false;
      for (var insideExpr : insideExprs) {

        var rangeExpr = insideExpr.computeRange(plan, windowsToTransform);
        if (!rangeExpr.isEmpty()) {
          atLeastOne = true;
          min = Duration.min(min, rangeExpr.minTimePoint().get());
          max = Duration.max(max, rangeExpr.maxTimePoint().get());
        }
      }

      if (atLeastOne) {
        //register new transformed window
        ret.add(Window.between(min, max));
      }
    }

    return ret;

  }
}
