package gov.nasa.jpl.aerie.scheduler.solver;

import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.goals.ChildCustody;
import gov.nasa.jpl.aerie.scheduler.goals.ActivityExistentialGoal;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * description of how well a plan satisfies its goals
 *
 * different schedulers may evaluate the same plan in different ways
 *
 * the evaluation includes any scoring metrics that the scheduler calculated
 * as well as useful metadata, eg which goals were satisfied by which activity instances
 */
public class Evaluation {

  /**
   * the set of all per-goal evaluations
   */
  protected final java.util.HashMap<Goal, GoalEvaluation> goalEvals = new java.util.HashMap<>();

  /**
   * description of the satisfaction of a single goal in isolation
   */
  public static class GoalEvaluation {
    /**
     * a map associating each activity that contributed to the goal to a boolean stating whether the goal created it or not
     */
    protected final java.util.Map<SchedulingActivityDirective, Boolean> acts = new java.util.HashMap<>();

    /**
     * the numeric evaluation score for the goal
     */
    protected double score = 0.0;

    /**
     * the number of conflicts originally detected
     */
    protected Integer nbConflictsDetected = null;

    /**
     * sets the numeric score for the evaluation of the goal
     *
     * more positive scores are more satisfied
     *
     * scores are only comparable if generaged by the same scheduler
     *
     * @param score the score to assign
     */
    public void setScore(double score) { this.score = score; }

    /**
     * fetches the numeric evaluation score for the goal
     *
     * @return the numeric evaluation score for the goal
     */
    public double getScore() { return score; }

    public void setNbConflictsDetected(final int nbConflictsDetected) {
      this.nbConflictsDetected = nbConflictsDetected;
    }

    public Optional<Integer> getNbConflictsDetected() {
      if(nbConflictsDetected == null){
        return Optional.empty();
      }
      return Optional.of(nbConflictsDetected);
    }

    /**
     * flags given activity as contributing to the goal's (dis)satisfaction
     *
     * @param act IN the activity instance that contributed to the goal's
     *     evaluation
     * @param createdByThisGoal IN a boolean stating whether the instance has been created by this goal or not
     */
    public void associate(SchedulingActivityDirective act, boolean createdByThisGoal) { acts.put(act, createdByThisGoal);}

    /**
     * flags all given activities as contributing to the goal's (dis)satisfaction
     *
     * @param acts IN container of activities that contributed to the goal's
     *     evaluation
     * @param createdByThisGoal IN a boolean stating whether the instance has been created by this goal or not
     */
    public void associate(java.util.Collection<SchedulingActivityDirective> acts, boolean createdByThisGoal) {
      acts.forEach(a ->this.acts.put(a, createdByThisGoal));
    }

    public void removeAssociation(java.util.Collection<SchedulingActivityDirective> acts){
      this.acts.entrySet().removeIf(act -> acts.contains(act.getKey()));
    }

    /**
     * fetches the set of all activities that contributed to the evaluation
     *
     * @return the set of all activities that contributed to the evaluation
     */
    public java.util.Collection<SchedulingActivityDirective> getAssociatedActivities() {
      return java.util.Collections.unmodifiableSet(acts.keySet());
    }
    /**
     * fetches the set of all activities that this goal inserted in the plan
     *
     * @return the set of all activities that this goal inserted in the plan
     */
    public java.util.Collection<SchedulingActivityDirective> getInsertedActivities() {
      return java.util.Collections.unmodifiableSet(acts.entrySet().stream().filter((a)-> a.getValue().equals(true)).map(
          Map.Entry::getKey).collect(
          Collectors.toSet()));
    }

  }

  /**
   * returns the (possibly new) evaluation for a given goal
   *
   * if there is no current evaluation for the given goal, a new empty
   * one is created and returned
   *
   * @param goal the goal to evaluate
   * @return the evaluation of the specified goal
   */
  public GoalEvaluation forGoal(Goal goal) {
    return goalEvals.computeIfAbsent(goal, k -> new GoalEvaluation());
  }

  /**
   * fetches the set of all goals evaluated
   *
   * @return the goals that are evaluated
   */
  public java.util.Collection<Goal> getGoals() {
    return goalEvals.keySet();
  }

  /**
   * fetch all goals and their current individual evaluation
   *
   * @return mapping from goals to their current individual evaluation (non-modifiable)
   */
  public java.util.Map<Goal,GoalEvaluation> getGoalEvaluations() { return Collections.unmodifiableMap(goalEvals); }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Evaluation that = (Evaluation) o;
    return Objects.equals(goalEvals, that.goalEvals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(goalEvals);
  }

  public boolean canAssociateMoreToCreatorOf(final SchedulingActivityDirective instance){
    final var creator$ = getGoalCreator(instance);
    // for now: all existing activities in the plan are allowed to be associated with any goal
    if (creator$.isEmpty()) return true;
    final var creator = creator$.get();

    if(!(creator instanceof ActivityExistentialGoal activityExistentialCreator)) return true;
      return activityExistentialCreator.getChildCustody() == ChildCustody.Jointly;//we can piggyback
  }

  /**
   * If an activity instance was already in the plan prior to this run of the scheduler, this method will return Optional.empty()
   */
  Optional<Goal> getGoalCreator(final SchedulingActivityDirective instance){
    for(final var goalEval : goalEvals.entrySet()){
      if(goalEval.getValue().getInsertedActivities().contains(instance)){
        return Optional.of(goalEval.getKey());
      }
    }
    return Optional.empty();
  }

}

