package gov.nasa.jpl.aerie.scheduler;

/**
 * a solution to a planning problem including a schedule of activities
 *
 * may only be a partial solution to the whole planning problem, ie some
 * goals may be left unsatisfied
 */
public interface Plan {

  /**
   * adds the given activity instances to the scheduled plan solution
   *
   * the provided instances must have start times specified
   *
   * @param acts IN the set of activity instances to schedule into the plan
   */
  void add(java.util.Collection<ActivityInstance> acts);

  /**
   * adds the given activity instance to the scheduled plan solution
   *
   * the provided instance must have start time specified
   *
   * @param act IN activity instance to schedule into the plan
   */
  void add(ActivityInstance act);
  /**
   * adds the given activity instances to the scheduled plan solution
   *
   * the provided instances must have start times specified
   *
   * @param acts IN the set of activity instances to remove from the plan
   */
  void remove(java.util.Collection<ActivityInstance> acts);

  /**
   * removes the given activity instance to the scheduled plan solution
   *
   * the provided instance must have start time specified
   *
   * @param act IN activity instance to remove from the plan
   */
  void remove(ActivityInstance act);

  void removeAllWindows();


  /**
   * fetches activities in the plan ordered by start time
   *
   * @return set of all activities in the plan ordered by start time
   */
  java.util.List<ActivityInstance> getActivitiesByTime();

  /**
   * fetches activities in the plan by type
   *
   * @return map of all activities in the plan by type
   */
  java.util.Map<String, java.util.List<ActivityInstance>> getActivitiesByType();

  /**
   * fetches activities in the plan by type
   *
   * @return map of all activities in the plan by type
   */
  java.util.Set<ActivityInstance> getActivities();

  /**
   * finds activity instances in the plan that meet the given criteria
   *
   * @param template IN the matching criteria to use on activity instances
   * @return collection of instances that match the given template
   */
  java.util.Collection<ActivityInstance> find(
      ActivityExpression template);

  /**
   * adds a new evaluation to the plan
   *
   * note that different solvers or metrics will have different evaluations
   * for the same plan
   *
   * @param eval IN the new evaluation to add to the plan
   */
  void addEvaluation(Evaluation eval);

  /**
   * fetches all of the evaluations posted to the plan
   *
   * @return container of all evaluations posted to the plan
   */
  Evaluation getEvaluation();

}
