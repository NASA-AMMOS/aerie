package gov.nasa.jpl.aerie.scheduler.constraints.transformers;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

public interface TimeWindowsTransformer {

  Windows transformWindows(Plan plan, Windows windows);

}
