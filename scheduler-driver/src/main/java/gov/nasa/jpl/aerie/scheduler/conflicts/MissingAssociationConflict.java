package gov.nasa.jpl.aerie.scheduler.conflicts;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;

import java.util.Collection;
import java.util.Optional;

public class MissingAssociationConflict extends Conflict {
  private final Collection<SchedulingActivityDirective> instances;

  private final Optional<Boolean> anchorToStart;
  private final Optional<SchedulingActivityDirectiveId> anchorIdTo;
  /**
   * ctor creates a new conflict
   *
   * @param goal IN STORED the dissatisfied goal that issued the conflict
   * @param instancesToChooseFrom IN STORED the list of instances to choose from to perform the association
   * @param anchorToStart IN STORED boolean indicating whether the anchor is associated to the START or the END
   * The value is used to support a Solver implementation to calculate the absolute START time offset
   */
  public MissingAssociationConflict(
      final Goal goal,
      final Collection<SchedulingActivityDirective> instancesToChooseFrom,
      final Optional<SchedulingActivityDirectiveId> anchorIdTo,
      final Optional<Boolean> anchorToStart) {


    super(goal, new EvaluationEnvironment());
    this.instances = instancesToChooseFrom;
    this.anchorIdTo = anchorIdTo;
    this.anchorToStart = anchorToStart;
  }

  public Collection<SchedulingActivityDirective> getActivityInstancesToChooseFrom(){
    return instances;
  }

  public Optional<SchedulingActivityDirectiveId> getAnchorIdTo() {
    return anchorIdTo;
  }

  public Optional<Boolean> getAnchorToStart() {
    return anchorToStart;
  }

  @Override
  public Windows getTemporalContext() {
    return null;
  }

  @Override
  public String toString(){
    return "Conflict: missing association between goal and 1 activity of following set: "
           + this.getActivityInstancesToChooseFrom() + ". Produced by goal " + getGoal().getName();
  }
}
