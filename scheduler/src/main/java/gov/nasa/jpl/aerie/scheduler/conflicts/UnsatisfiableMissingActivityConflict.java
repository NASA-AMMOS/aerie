package gov.nasa.jpl.aerie.scheduler.conflicts;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;

public class UnsatisfiableMissingActivityConflict extends Conflict {


  /**
   * ctor creates a new conflict
   *
   * @param goal IN STORED the dissatisfied goal that issued the conflict
   */
  public UnsatisfiableMissingActivityConflict(Goal goal) {
    super(goal);
  }

  @Override
  public Windows getTemporalContext() {
    return null;
  }
}
