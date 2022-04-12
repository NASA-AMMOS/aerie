package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.ArrayList;
import java.util.List;

/**
 * A latching filter is a filter applying a different filter on the first element of a sequence and another filter on
 * the
 * remaining element of the sequence
 */
public class FilterLatching implements TimeWindowsFilter {

  private final FilterFunctional firstFilter;
  private final FilterFunctional otherFilter;

  public FilterLatching(FilterFunctional filter1, FilterFunctional filter2) {
    firstFilter = filter1;
    otherFilter = filter2;
  }


  @Override
  public Windows filter(Plan plan, Windows windows) {
    List<Window> ret = new ArrayList<>();

    if (!windows.isEmpty()) {

      boolean first = true;

      for (var subint : windows) {
        if (first) {
          if (firstFilter.shouldKeep(plan, subint)) {
            ret.add(subint);
            first = false;
          }
        } else {
          if (otherFilter.shouldKeep(plan, subint)) {
            ret.add(subint);
          }
        }
      }

    }

    return new Windows(ret);
  }
}
