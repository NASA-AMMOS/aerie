package gov.nasa.jpl.aerie.scheduler;

import java.util.Collections;
import java.util.Objects;

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
   * description of the satisfaction of a single goal in isolation
   */
  public static class GoalEvaluation {

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

    /**
     * the numeric evaluation score for the goal
     */
    protected double score = 0.0;

    /**
     * flags given activity as contributing to the goal's (dis)satisfaction
     *
     * @param act IN the activity instance that contributed to the goal's
     *     evaluation
     */
    public void associate(ActivityInstance act) { acts.add(act); }

    /**
     * flags all given activities as contributing to the goal's (dis)satisfaction
     *
     * @param acts IN container of activities that contributed to the goal's
     *     evaluation
     */
    public void associate(java.util.Collection<ActivityInstance> acts) {
      this.acts.addAll(acts);
    }

    /**
     * fetches the set of all activities that contributed to the evaluation
     *
     * @return the set of all activities that contributed to the evaluation
     */
    public java.util.Collection<ActivityInstance> getAssociatedActivities() {
      return java.util.Collections.unmodifiableSet(acts);
    }

    /**
     * the set of all activities that contributed to the evaluation
     */
    protected final java.util.Set<ActivityInstance> acts = new java.util.HashSet<>();

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

  /**
   * the set of all per-goal evaluations
   */
  protected final java.util.HashMap<Goal, GoalEvaluation> goalEvals = new java.util.HashMap<>();

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


}

