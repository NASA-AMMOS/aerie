package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;


/**
 * this filter turns any filter into a filter with resets
 */
public class FilterWithReset implements TimeWindowsFilter {


  public FilterWithReset(TimeRangeExpression reset, TimeWindowsFilter filter) {
    this.filter = filter;
    this.resetExpr = reset;
  }

  final TimeWindowsFilter filter;
  final TimeRangeExpression resetExpr;

  @Override
  public Windows filter(Plan plan, Windows windowsToFilter) {

    Windows ret = new Windows();

    int totalFiltered = 0;

    if (!windowsToFilter.isEmpty()) {

      var resetPeriods = resetExpr.computeRange(plan, new Windows(Window.FOREVER));

      for (var window : resetPeriods) {
        // get windows to filter that are completely contained in reset period
        Windows cur = windowsToFilter.subsetContained(window);
        if (!cur.isEmpty()) {
          //apply filter and union result
          ret.addAll(filter.filter(plan, cur));
          totalFiltered += cur.size();
        }
        //short circuit
        if (totalFiltered >= windowsToFilter.size()) {
          break;
        }
      }
    }

    return ret;
  }


}
