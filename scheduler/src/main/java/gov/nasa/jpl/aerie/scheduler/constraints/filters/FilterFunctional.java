package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.ArrayList;
import java.util.List;

public abstract class FilterFunctional implements TimeWindowsFilter {


  @Override
  public Windows filter(final SimulationResults simulationResults, final Plan plan, final Windows windows) {
    final List<Window> ret = new ArrayList<>();
    for (var window : windows) {
      if (shouldKeep(simulationResults, plan, window)) {
        ret.add(window);
      }
    }
    return new Windows(ret);
  }


  public abstract boolean shouldKeep(final SimulationResults simulationResults, final Plan plan, final Interval range);
}
