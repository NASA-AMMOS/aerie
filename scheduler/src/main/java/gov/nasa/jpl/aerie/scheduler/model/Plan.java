package gov.nasa.jpl.aerie.scheduler.model;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.solver.Evaluation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  void add(Collection<ActivityInstance> acts);

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
  void remove(Collection<ActivityInstance> acts);

  /**
   * removes the given activity instance to the scheduled plan solution
   *
   * the provided instance must have start time specified
   *
   * @param act IN activity instance to remove from the plan
   */
  void remove(ActivityInstance act);

  /**
   * fetches activities in the plan ordered by start time
   *
   * @return set of all activities in the plan ordered by start time
   */
  List<ActivityInstance> getActivitiesByTime();

  /**
   * fetches activities in the plan by type
   *
   * @return map of all activities in the plan by type
   */
  Map<ActivityType, List<ActivityInstance>> getActivitiesByType();

  /**
   * fetches activities in the plan by id
   *
   * @return map of all activities in the plan by id
   */
  Map<SchedulingActivityInstanceId, ActivityInstance> getActivitiesById();

  /**
   * fetches activities in the plan
   *
   * @return set of all activities in the plan
   */
  Set<ActivityInstance> getActivities();

  /**
   * finds activity instances in the plan that meet the given criteria
   *
   * @param template IN the matching criteria to use on activity instances
   * @return collection of instances that match the given template
   */
  Collection<ActivityInstance> find(
      ActivityExpression template, SimulationResults simulationResults);
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
   * fetches evaluation posted to the plan
   *
   * @return evaluation posted to the plan
   */
  Evaluation getEvaluation();

}
