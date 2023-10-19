package gov.nasa.jpl.aerie.scheduler.conflicts;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;

import java.util.Collection;

/**
 * describes plan problem due to lack of aan anchor
 * such conflicts are typically addressed by creating an anchor between the activity directive and the activity returned by finder
 */
public class MissingAnchorConflict extends Conflict {

  private final Collection<SchedulingActivityDirective> instances;
  private final SchedulingActivityDirectiveId anchorIdTo;

  /**
   * ctor creates a new conflict
   *
   * @param goal IN STORED the dissatisfied goal that issued the conflict
   * @param instancesToChooseFrom IN the list of instances to choose from to perform the association
   *
   */
  public MissingAnchorConflict(final Goal goal, final Collection<SchedulingActivityDirective> instancesToChooseFrom, SchedulingActivityDirectiveId anchorIdTo) {
    super(goal, new EvaluationEnvironment());
    this.instances = instancesToChooseFrom;
    this.anchorIdTo = anchorIdTo;
  }

  public SchedulingActivityDirectiveId getAnchorIdTo() {
    return anchorIdTo;
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
