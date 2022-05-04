package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

/**
 * a filter selects a subset of windows
 */
public interface TimeWindowsFilter {

  Windows filter(SimulationResults simulationResults, Plan plan, Windows windowsToFilter);


}
