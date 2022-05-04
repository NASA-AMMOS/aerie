package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter windows that have at least another window preceding ending within a delay
 */
public class FilterSequenceMaxGapBefore implements TimeWindowsFilter {

  private final Duration delay;

  public FilterSequenceMaxGapBefore(Duration delay) {
    this.delay = delay;
  }

  @Override
  public Windows filter(SimulationResults simulationResults, Plan plan, Windows windows) {
    Window before = null;
    List<Window> filtered = new ArrayList<>();
    for (var range : windows) {
      if (before != null) {
        if (range.start.minus(before.end).compareTo(delay) <= 0) {
          filtered.add(range);
        }
      }
      before = range;
    }
    return new Windows(filtered);
  }


}
