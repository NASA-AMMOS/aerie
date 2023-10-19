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
  private Optional<SchedulingActivityDirectiveId> anchorToId;

  /**
   * ctor creates a new conflict
   *
   * @param goal IN STORED the dissatisfied goal that issued the conflict
   * @param instancesToChooseFrom IN the list of instances to choose from to perform the association
   */
  public MissingAssociationConflict(final Goal goal, final Collection<SchedulingActivityDirective> instancesToChooseFrom) {
    super(goal, new EvaluationEnvironment());
    this.instances = instancesToChooseFrom;
    this.anchorToId = Optional.empty();
  }

  public MissingAssociationConflict(final Goal goal, final Collection<SchedulingActivityDirective> instancesToChooseFrom, SchedulingActivityDirectiveId anchorToId) {
    super(goal, new EvaluationEnvironment());
    this.instances = instancesToChooseFrom;
    this.anchorToId = Optional.ofNullable(anchorToId);
  }

  public Collection<SchedulingActivityDirective> getActivityInstancesToChooseFrom(){
    return instances;
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
