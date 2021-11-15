package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * A latching filter is a filter applying a different filter on the first element of a sequence and another filter on
 * the
 * remaining element of the sequence
 */
public class FilterLatching implements TimeWindowsFilter {

  private FilterFunctional firstFilter;
  private FilterFunctional otherFilter;

  public FilterLatching(FilterFunctional filter1, FilterFunctional filter2) {
    firstFilter = filter1;
    otherFilter = filter2;
  }


  @Override
  public TimeWindows filter(Plan plan, TimeWindows windows) {
    List<Range<Time>> ret = new ArrayList<Range<Time>>();

    if (!windows.isEmpty()) {

      boolean first = true;

      for (var subint : windows.getRangeSet()) {
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

    return TimeWindows.of(ret, true);
  }
}
