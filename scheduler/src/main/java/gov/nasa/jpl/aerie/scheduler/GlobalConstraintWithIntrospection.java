package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Windows;

/**
 * Abstract class defining methods that must be implemented by global constraints such as mutex or cardinality
 * Also provides a directory for creating these constraints
 */
public abstract class GlobalConstraintWithIntrospection extends GlobalConstraint {

  //specific to introspectable constraint : find the windows in which we can insert activities without violating
  //the constraint
  public abstract Windows findWindows(Plan plan, Windows windows, Conflict conflict);


}
