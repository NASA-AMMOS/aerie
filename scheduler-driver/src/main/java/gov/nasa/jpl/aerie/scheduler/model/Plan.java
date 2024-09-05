package gov.nasa.jpl.aerie.scheduler.model;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplate;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplateData;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;

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
   * Duplicates a plan
   * @return the duplicate plan
   */
  Plan duplicate();

  /**
   * adds the given activity instances to the scheduled plan solution
   *
   * the provided instances must have start times specified
   *
   * @param acts IN the set of activity instances to schedule into the plan
   */
  void add(Collection<SchedulingActivityDirective> acts);

  /**
   * adds the given activity instance to the scheduled plan solution
   *
   * the provided instance must have start time specified
   *
   * @param act IN activity instance to schedule into the plan
   */
  void add(SchedulingActivityDirective act);

  /**
   * adds the given TaskNetTemplate to the plan
   * @param tn IN TaskNetTemplate to be added to the plan
   */
  void addTaskNetTemplateData(final TaskNetTemplateData tn);

  /**
   * adds the given activity instances to the scheduled plan solution
   *
   * the provided instances must have start times specified
   *
   * @param acts IN the set of activity instances to remove from the plan
   */
  void remove(Collection<SchedulingActivityDirective> acts);

  /**
   * removes the given activity instance to the scheduled plan solution
   *
   * the provided instance must have start time specified
   *
   * @param act IN activity instance to remove from the plan
   */
  void remove(SchedulingActivityDirective act);

  /**
   * removes the given TaskNetTemplate from the plan
   * @param tn IN TaskNetTemplate to be removed from the plan
   */
  void removeTaskNetTemplate(final TaskNetTemplate tn);

  /**
   * replace and old activity by a new one
   * @param oldAct Old Activity
   * @param newAct New Activity
   */
  void replaceActivity(SchedulingActivityDirective oldAct, SchedulingActivityDirective newAct);

  /**
   * fetches activities in the plan ordered by start time
   *
   * @return set of all activities in the plan ordered by start time
   */
  List<SchedulingActivityDirective> getActivitiesByTime();

  /**
   * fetches activities in the plan by type
   *
   * @return map of all activities in the plan by type
   */
  Map<ActivityType, List<SchedulingActivityDirective>> getActivitiesByType();

  /**
   * fetches activities in the plan by id
   *
   * @return map of all activities in the plan by id
   */
  Map<SchedulingActivityDirectiveId, SchedulingActivityDirective> getActivitiesById();

  /**
   * fetches activities in the plan
   *
   * @return set of all activities in the plan
   */
  Set<SchedulingActivityDirective> getActivities();

  /**
  * @return the set of anchors from all activities in the plan
   */
  Set<SchedulingActivityDirectiveId> getAnchorIds();

  /**
   * finds activity instances in the plan that meet the given criteria
   *
   * @param template IN the matching criteria to use on activity instances
   * @return collection of instances that match the given template
   */
  Collection<SchedulingActivityDirective> find(
      ActivityExpression template, SimulationResults simulationResults, EvaluationEnvironment evaluationEnvironment);

  Duration calculateAbsoluteStartOffsetAnchoredActivity(SchedulingActivityDirective actAnchorTo);
}
