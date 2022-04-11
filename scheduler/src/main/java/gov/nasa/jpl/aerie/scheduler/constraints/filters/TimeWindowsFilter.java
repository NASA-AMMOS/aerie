package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

/**
 * a filter selects a subset of windows
 */
public interface TimeWindowsFilter {

  Windows filter(Plan plan, Windows windowsToFilter);


}
