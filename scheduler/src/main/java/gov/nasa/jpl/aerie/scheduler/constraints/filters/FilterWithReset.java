package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.scheduler.model.Plan;


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
  public Windows filter(SimulationResults simulationResults, Plan plan, Windows windowsToFilter) {

    Windows ret = new Windows();

    int totalFiltered = 0;

    if (!windowsToFilter.isEmpty()) {

      var resetPeriods = resetExpr.computeRange(simulationResults, plan, new Windows(Window.FOREVER));

      for (var window : resetPeriods) {
        // get windows to filter that are completely contained in reset period
        Windows cur = windowsToFilter.subsetContained(window);
        if (!cur.isEmpty()) {
          //apply filter and union result
          ret.addAll(filter.filter(simulationResults, plan, cur));
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
