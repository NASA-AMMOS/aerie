package gov.nasa.jpl.aerie.scheduler;

/**
 * describes an issue in a plan caused by a specific activity instance missing
 *
 * such conflicts are typically addressed by scheduling the specific instance
 */
public class MissingActivityInstanceConflict extends MissingActivityConflict {

  /**
   * ctor creates a new conflict regarding a missing activity instance
   *
   * REVIEW: do we want an ActivityInstanceGoal type?
   *
   * @param goal IN STORED the dissatisfied goal that issued the conflict
   * @param instance IN STORED the specific activity instance that is
   *     desired in the plan
   */
  public MissingActivityInstanceConflict(
      ActivityExistentialGoal goal,
      ActivityInstance instance)
  {
    super(goal);

    if (instance == null) {
      throw new IllegalArgumentException(
          "creating specific missing instance conflict required non-null instance");
    }
    this.instance = instance;
  }

  @Override
  public String toString() {
    if (this.instance != null) {
      return "Conflict : missing activity instance " + this.instance.toString();
    }
    return "Empty conflict";
  }

  /**
   * {@inheritDoc}
   *
   * FINISH: fix up documentation
   * the time at which the desired activity would start
   *
   * does not consider other constraints on the possible activity, eg timing
   * with respect to other events or allowable state transitions or even
   * the activity's own duration limits
   *
   * the times encompass just the desired start times of the missing activity
   */
  @Override
  public TimeWindows getTemporalContext() {
    return TimeWindows.of(instance.getStartTime());
  }

  /**
   * the goal whose dissatisfaction initially created this conflict
   *
   * @return reference to the dissatisfied goal that caused this conflict
   */
  @Override
  public ActivityExistentialGoal getGoal() {
    return (ActivityExistentialGoal) super.getGoal();
  }

  /**
   * fetches the specifically requested instance that is desired
   *
   * @return the specifically requested instance that is desired
   */
  public ActivityInstance getInstance() {
    return instance;
  }

  /**
   * the specific activity instance that is desired in the plan
   */
  protected ActivityInstance instance;

}
