package gov.nasa.jpl.aerie.scheduler;

/**
 * a filter selects a subset of windows
 */
public interface TimeWindowsFilter {

  public TimeWindows filter(Plan plan, TimeWindows windowsToFilter);


}
