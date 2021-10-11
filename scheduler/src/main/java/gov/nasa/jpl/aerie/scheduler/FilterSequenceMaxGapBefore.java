package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Filter windows that have at least another window preceding ending within a delay
 */
public class FilterSequenceMaxGapBefore implements TimeWindowsFilter {

  private Duration delay;

  public FilterSequenceMaxGapBefore(Duration delay) {
    this.delay = delay;
  }

  @Override
  public TimeWindows filter(Plan plan, TimeWindows windows) {
    Range<Time> before = null;
    Collection<Range<Time>> filtered = new ArrayList<Range<Time>>();
    for (Range<Time> range : windows.getRangeSet()) {
      if (before != null) {
        if (range.getMinimum().minus(before.getMaximum()).compareTo(delay) <= 0) {
          filtered.add(range);
        }
      }
      before = range;
    }
    return TimeWindows.of(filtered, true);
  }


}
