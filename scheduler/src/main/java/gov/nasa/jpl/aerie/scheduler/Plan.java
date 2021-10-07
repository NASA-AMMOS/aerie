package gov.nasa.jpl.aerie.scheduler;

/**
 * a solution to a planning problem including a schedule of activities
 *
 * may only be a partial solution to the whole planning problem, ie some
 * goals may be left unsatisfied
 */
public interface Plan {

  /**
   * returns the mission model that the plan is based on
   *
   * @return the mission model that the plan is based on
   */
  public MissionModel getMissionModel();

  /**
   * adds the given activity instances to the scheduled plan solution
   *
   * the provided instances must have start times specified
   *
   * @param acts IN the set of activity instances to schedule into the plan
   */
  public void add(java.util.Collection<ActivityInstance> acts);

  /**
   * adds the given activity instance to the scheduled plan solution
   *
   * the provided instance must have start time specified
   *
   * @param act IN activity instance to schedule into the plan
   */
  public void add(ActivityInstance act);
  /**
   * adds the given activity instances to the scheduled plan solution
   *
   * the provided instances must have start times specified
   *
   * @param acts IN the set of activity instances to remove from the plan
   */
  public void remove(java.util.Collection<ActivityInstance> acts);

  /**
   * removes the given activity instance to the scheduled plan solution
   *
   * the provided instance must have start time specified
   *
   * @param act IN activity instance to remove from the plan
   */
  public void remove(ActivityInstance act);

  public void removeAllWindows();

  /**
   * adds the given state value history to the scheduled plan solution
   *
   * @param stateTimeline IN the state value history to add to the plan
   * @param <T> the value type of the state
   */
  public <T extends Comparable<T>> void add(State<T> stateTimeline);

  /**
   * fetches activities in the plan ordered by start time
   *
   * @return set of all activities in the plan ordered by start time
   */
  public java.util.List<ActivityInstance> getActivitiesByTime();

  /**
   * fetches activities in the plan by type
   *
   * @return map of all activities in the plan by type
   */
  public java.util.Map<String, java.util.List<ActivityInstance>> getActivitiesByType();

  /**
   * finds activity instances in the plan that meet the given criteria
   *
   * @param template IN the matching criteria to use on activity instances
   * @return collection of instances that match the given template
   */
  public java.util.Collection<ActivityInstance> find(
      ActivityExpression template);

  /**
   * adds a new evaluation to the plan
   *
   * note that different solvers or metrics will have different evaluations
   * for the same plan
   *
   * @param eval IN the new evaluation to add to the plan
   */
  public void addEvaluation(Evaluation eval);

  /**
   * fetches all of the evaluations posted to the plan
   *
   * @return container of all evaluations posted to the plan
   */
  public java.util.Collection<Evaluation> getEvaluations();

}
