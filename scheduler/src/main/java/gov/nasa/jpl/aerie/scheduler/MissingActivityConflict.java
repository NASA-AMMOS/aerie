package gov.nasa.jpl.aerie.scheduler;

/**
 * describes an issue in a plan whereby a desired activity instance is absent
 *
 * such conflicts are typically addressed by scheduling additional activities
 */
public abstract class MissingActivityConflict extends Conflict {

  /**
   * ctor creates a new conflict regarding a missing activity
   *
   * @param goal IN STORED the dissatisfied goal that issued the conflict
   */
  public MissingActivityConflict(
      ActivityExistentialGoal goal)
  {
    super(goal);
  }

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
   */
  @Override
  public abstract TimeWindows getTemporalContext();


}
