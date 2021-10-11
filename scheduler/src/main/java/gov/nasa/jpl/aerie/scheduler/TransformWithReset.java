package gov.nasa.jpl.aerie.scheduler;

import java.util.List;


/**
 * this filter turns any filter into a filter with resets
 */
public class TransformWithReset implements TimeWindowsTransformer {


  public TransformWithReset(TimeRangeExpression reset, TimeWindowsTransformer filter) {
    this.transform = filter;
    this.resetExpr = reset;
  }

  TimeWindowsTransformer transform;
  TimeRangeExpression resetExpr;

  @Override
  public TimeWindows transformWindows(Plan plan, TimeWindows windowsToTransform) {

    TimeWindows ret = new TimeWindows();
    int totalFiltered = 0;

    if (!windowsToTransform.isEmpty()) {

      List<Range<Time>> resetPeriods = resetExpr.computeRange(plan, TimeWindows.spanMax()).getRangeSet();

      for (var window : resetPeriods) {
        //System.out.println("RESET " + window);
        // get windows to filter that are completely contained in reset period
        TimeWindows cur = windowsToTransform.subsetFullyContained(window);
        if (!cur.isEmpty()) {
          //apply filter and union result
          ret.union(transform.transformWindows(plan, cur));
          totalFiltered += cur.size();
        }
        //short circuit
        if (totalFiltered >= windowsToTransform.size()) {
          break;
        }
      }
    }

    //System.out.println(ret);
    return ret;
  }


}
