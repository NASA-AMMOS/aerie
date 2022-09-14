package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.Optional;

public abstract class FilterFunctional implements TimeWindowsFilter {


  @Override
  public Windows filter(final SimulationResults simulationResults, final Plan plan, final Windows windows) {
    final Windows ret = new Windows(windows);
    for (final var interval: windows.iterateTrue()) {
      if (!shouldKeep(simulationResults, plan, interval)) {
        ret.set(interval, false);
      }
    }
    return new Windows(ret);
  }


  public abstract boolean shouldKeep(final SimulationResults simulationResults, final Plan plan, final Interval range);
}
