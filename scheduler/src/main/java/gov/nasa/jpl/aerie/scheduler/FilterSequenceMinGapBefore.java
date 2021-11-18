package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter in windows that have another window preceding separated at least by delay
 */

// delay = 4
// passW:  ......[======]...[=======]............[=====].......
// output: ......[======]........................[=====].......
public class FilterSequenceMinGapBefore implements TimeWindowsFilter {

  private Duration delay;

  public FilterSequenceMinGapBefore(Duration delay) {
    this.delay = delay;
  }

  @Override
  public Windows filter(Plan plan, Windows windows) {
    Window before = null;
    List<Window> filtered = new ArrayList<>();
    var windowsToFilter = windows;
    if (windowsToFilter.size() > 0) {
      filtered.add(windowsToFilter.iterator().next());
      for (var range : windowsToFilter) {
        if (before != null) {
          if (range.start.minus(before.end).compareTo(delay) >= 0) {
            filtered.add(range);
          }
        }
        before = range;
      }
    }
    return new Windows(filtered);
  }


}
