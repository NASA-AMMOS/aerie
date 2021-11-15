package gov.nasa.jpl.aerie.scheduler;

import java.util.List;


/**
 * this filter turns any filter into a filter with resets
 */
public class FilterWithReset implements TimeWindowsFilter {


  public FilterWithReset(TimeRangeExpression reset, TimeWindowsFilter filter) {
    this.filter = filter;
    this.resetExpr = reset;
  }

  TimeWindowsFilter filter;
  TimeRangeExpression resetExpr;

  @Override
  public TimeWindows filter(Plan plan, TimeWindows windowsToFilter) {

    TimeWindows ret = new TimeWindows(true);

    int totalFiltered = 0;

    if (!windowsToFilter.isEmpty()) {

      List<Range<Time>> resetPeriods = resetExpr.computeRange(plan, TimeWindows.spanMax()).getRangeSet();

      for (var window : resetPeriods) {
        // get windows to filter that are completely contained in reset period
        TimeWindows cur = windowsToFilter.subsetFullyContained(window);
        if (!cur.isEmpty()) {
          //apply filter and union result
          ret.union(filter.filter(plan, cur));
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
