package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Windows;


/**
 * this filter turns any filter into a filter with resets
 */
public class TransformWithReset implements TimeWindowsTransformer {


  public TransformWithReset(TimeRangeExpression reset, TimeWindowsTransformer filter) {
    this.transform = filter;
    this.resetExpr = reset;
  }

  private final TimeWindowsTransformer transform;
  private final TimeRangeExpression resetExpr;

  @Override
  public Windows transformWindows(Plan plan, Windows windowsToTransform) {

    Windows ret = new Windows();
    int totalFiltered = 0;

    if (!windowsToTransform.isEmpty()) {

      var resetPeriods = resetExpr.computeRange(plan, Windows.forever());

      for (var window : resetPeriods) {
        // get windows to filter that are completely contained in reset period
        Windows cur = windowsToTransform.subsetContained(window);
        if (!cur.isEmpty()) {
          //apply filter and union result
          ret.addAll(transform.transformWindows(plan, cur));
          totalFiltered += cur.size();
        }
        //short circuit
        if (totalFiltered >= windowsToTransform.size()) {
          break;
        }
      }
    }

    return ret;
  }


}
