package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Windows;

public class AlwaysGlobalConstraint extends GlobalConstraint {

  private final StateConstraint<?> sc;

  public AlwaysGlobalConstraint(StateConstraint<?> sc) {
    this.sc = sc;
    throw new IllegalArgumentException("Not implemented");

  }

  @Override
  public ConstraintState isEnforced(Plan plan, Windows windows) {
    throw new IllegalArgumentException("Not implemented");
  }

}
