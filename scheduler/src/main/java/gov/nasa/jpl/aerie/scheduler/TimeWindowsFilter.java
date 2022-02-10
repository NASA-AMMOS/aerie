package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Windows;

/**
 * a filter selects a subset of windows
 */
public interface TimeWindowsFilter {

  Windows filter(Plan plan, Windows windowsToFilter);


}
