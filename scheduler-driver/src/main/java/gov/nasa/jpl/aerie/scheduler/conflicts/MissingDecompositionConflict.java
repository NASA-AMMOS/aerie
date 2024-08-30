package gov.nasa.jpl.aerie.scheduler.conflicts;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.ActivityReference;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivity;

/**
 * describes an issue in a plan whereby a desired activity instance is absent
 *
 * such conflicts are typically addressed by scheduling additional activities
 */
public class MissingDecompositionConflict extends Conflict {
  //private final ActivityType actType;
  private ActivityReference activity;
  public Interval startInterval;
  public Interval endInterval;

  /**
   * ctor creates a new conflict regarding a missing activity
   *
   * @param goal IN STORED the dissatisfied goal that issued the conflict
   */
  public MissingDecompositionConflict(
      final Goal goal,
      EvaluationEnvironment environment,
      ActivityReference activity) {
    super(goal, environment);
    this.activity = activity;
    this.startInterval = null;
    this.endInterval = null;
  }

  public MissingDecompositionConflict(
      final Goal goal,
      EvaluationEnvironment environment,
      ActivityReference activity,
      Interval startInterval, Interval endInterval) {
    super(goal, environment);
    this.activity = activity;
    this.startInterval = startInterval;
    this.endInterval = endInterval;
  }

  public MissingDecompositionConflict(
      final Goal goal,
      EvaluationEnvironment environment,
      SchedulingActivity activity
  ){
    super(goal, environment);
    this.activity = new ActivityReference(activity.getType().getName(), activity.arguments());
  }

  public ActivityReference getActivityType(){return activity;}

  /**
   * {@inheritDoc}
   *
   * the times over which the activity's absence is itself causing a problem
   *
   * does not consider other constraints on the possible activity, eg timing
   * with respect to other events or allowable state transitions or even
   * the activity's own duration limits
   *
   * the times encompass both the start and end of the missing activity (they
   * are not just start windows)
   *
   * REVIEW: maybe better to just have start windows? gets confusing and easy
   * to mix up
   * @return
   */
  @Override
  public Windows getTemporalContext(){ return null;};

}
