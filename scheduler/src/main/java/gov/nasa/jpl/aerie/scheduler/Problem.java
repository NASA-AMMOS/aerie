package gov.nasa.jpl.aerie.scheduler;

/**
 * description of a planning problem to be solved
 */
public class Problem {

  /**
   * creates a new empty problem based in the given mission model
   *
   * @param mission IN the mission model that this problem is based on
   */
  public Problem(MissionModelWrapper mission) {
    if (mission == null) {
      throw new IllegalArgumentException(
          "creating problem descriptor with null mission model");
    }
    this.mission = mission;
    this.initialPlan = new PlanInMemory(mission);
  }

  /**
   * the mission model that this problem is based on
   */
  private MissionModelWrapper mission;

  /**
   * the initial seed plan to start scheduling from
   */
  private Plan initialPlan;

  /**
   * container of all goals in the problem, indexed by name
   */
  private java.util.HashMap<String, Goal> goalsByName = new java.util.HashMap<>();

  /**
   * fetches the mission model that this problem is based on
   *
   * @return the mission model that this problem is based on
   */
  public MissionModelWrapper getMissionModel() {
    return mission;
  }

  /**
   * fetches the initial seed plan that schedulers may start from
   *
   * @return the initial seed plan that schedulers may start from
   */
  public Plan getInitialPlan() {
    return initialPlan;
  }

  /**
   * sets the initial seed plan that schedulers may start from
   *
   * @param plan the initial seed plan that schedulers may start from
   */
  public void setInitialPlan(Plan plan) {
    initialPlan = plan;
  }

  /**
   * adds a new goal to the problem specification
   *
   * @param goal IN the new goal to add to the problem
   */
  public void add(Goal goal) {
    if (goal == null) {
      throw new IllegalArgumentException(
          "inserting null goal into problem");
    }
    final var goalName = goal.getName();
    assert goalName != null;
    if (goalsByName.containsKey(goalName)) {
      throw new IllegalArgumentException(
          "inserting goal with duplicate name=" + goalName + " into problem");
    }
    goalsByName.put(goalName, goal);
  }

  /**
   * retrieves the set of all requested plan goals
   *
   * planning algorithms should attempt to satisfy these goals when generating
   * or updating plans. the details of exactly how the goals are weighted
   * against eachother is up to the algorithm, and not all goals must be
   * satisfied in a proposed solution plan
   *
   * @return an un-modifiable container of the goals requested for this plan
   */
  public java.util.Collection<Goal> getGoals() {
    return java.util.Collections.unmodifiableCollection(goalsByName.values());
  }


}
