package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

public abstract class FilterFunctional implements TimeWindowsFilter {


  @Override
  public TimeWindows filter(Plan plan, TimeWindows windows) {
    List<Range<Time>> ret = new ArrayList<Range<Time>>();
    for (var window : windows.getRangeSet()) {
      if (shouldKeep(plan, window)) {
        ret.add(window);
      }
    }
    return TimeWindows.of(ret, true);
  }


  public abstract boolean shouldKeep(Plan plan, Range<Time> range);
}
