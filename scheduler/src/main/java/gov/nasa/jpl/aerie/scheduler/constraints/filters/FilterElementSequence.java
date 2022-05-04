package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Filters elements in a sequence of windows
 * Several flavors :
 * - keeps first element
 * - keep last element
 * - keep an element at a given position (-1 is the last element, -2 is the element before the last etc)
 */
public class FilterElementSequence implements TimeWindowsFilter {


  private final int elementIndex;

  private FilterElementSequence(int numberInSequence) {
    elementIndex = numberInSequence;
  }

  public static FilterElementSequence first() {
    return new FilterElementSequence(0);
  }

  public static FilterElementSequence last() {
    return new FilterElementSequence(-1);
  }

  public static FilterElementSequence numbered(int i) {
    return new FilterElementSequence(i);
  }

  @Override
  public Windows filter(SimulationResults simulationResults, Plan plan, Windows windows) {
    List<Window> ret = new ArrayList<>();

    if (!windows.isEmpty()) {

      List<Window> ranges = StreamSupport
          .stream(windows.spliterator(), false)
          .collect(Collectors.toList());
      if (this.elementIndex >= 0 && this.elementIndex < ranges.size()) {
        ret.add(ranges.get(this.elementIndex));
      } else if (elementIndex < 0 && Math.abs(this.elementIndex) <= ranges.size()) {
        ret.add(ranges.get(ranges.size() + elementIndex));
      }
    }

    return new Windows(ret);
  }
}
