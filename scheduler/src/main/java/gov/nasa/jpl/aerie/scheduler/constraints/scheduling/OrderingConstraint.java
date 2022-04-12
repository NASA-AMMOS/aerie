package gov.nasa.jpl.aerie.scheduler.constraints.scheduling;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;

/**
 * Work in progress for specialization of globalconstraint for ordering constraints
 */
public class OrderingConstraint extends GlobalConstraintWithIntrospection {

  ActivityType actType;
  ActivityType otherActType;

  public static BinaryMutexConstraint buildMutexConstraint(ActivityType type1, ActivityType type2) {
    BinaryMutexConstraint mc = new BinaryMutexConstraint();
    mc.fill(type1, type2);
    return mc;
  }

  public Windows findWindows(Plan plan, Windows windows, ActivityType actToBeScheduled) {
    return null;
  }

  protected void fill(ActivityType type1, ActivityType type2) {
    this.actType = type1;
    this.otherActType = type2;
  }

  protected OrderingConstraint() {
  }


  @Override
  public ConstraintState isEnforced(Plan plan, Windows windows) {
    return null;
  }

  @Override
  public Windows findWindows(Plan plan, Windows windows, Conflict conflict) {
    return null;
  }
}
