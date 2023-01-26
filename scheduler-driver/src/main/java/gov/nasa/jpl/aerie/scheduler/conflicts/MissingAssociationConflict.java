package gov.nasa.jpl.aerie.scheduler.conflicts;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;

import java.util.Collection;

public class MissingAssociationConflict extends Conflict {
  private final Collection<ActivityInstance> instances;

  /**
   * ctor creates a new conflict
   *
   * @param goal IN STORED the dissatisfied goal that issued the conflict
   * @param instancesToChooseFrom IN the list of instances to choose from to perform the association
   */
  public MissingAssociationConflict(final Goal goal, final Collection<ActivityInstance> instancesToChooseFrom) {
    super(goal, new EvaluationEnvironment());
    this.instances = instancesToChooseFrom;
  }

  public Collection<ActivityInstance> getActivityInstancesToChooseFrom(){
    return instances;
  }

  @Override
  public Windows getTemporalContext() {
    return null;
  }
}
