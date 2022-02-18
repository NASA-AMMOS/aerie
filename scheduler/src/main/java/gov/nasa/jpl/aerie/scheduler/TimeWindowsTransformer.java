package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Windows;

public interface TimeWindowsTransformer {

  Windows transformWindows(Plan plan, Windows windows);

}
