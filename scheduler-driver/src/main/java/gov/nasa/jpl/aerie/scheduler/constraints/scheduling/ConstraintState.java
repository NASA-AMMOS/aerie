package gov.nasa.jpl.aerie.scheduler.constraints.scheduling;

import gov.nasa.jpl.aerie.constraints.time.Windows;

/**
 * Class similar to Conflict but for GlobalConstraints
 */
public class ConstraintState {

  /**
   * constraint concerned by this state
   */
  public final GlobalConstraint constraint;

  /**
   * boolean stating whether the constraint is violated or not
   */
  public boolean isViolation = true;

  /**
   * intervals during which the constraint is violated
   */
  public final Windows violationWindows;

  // readable explanation when possible
  public String cause;

  public ConstraintState(
      GlobalConstraint constraint, boolean isViolation, Windows violationWindows) {
    this.isViolation = isViolation;
    this.violationWindows = violationWindows;
    this.constraint = constraint;
  }
}
