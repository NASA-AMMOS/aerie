package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * description of a planning problem to be solved
 */
public class Problem {

  /**
   * the mission model that this problem is based on
   */
  private final MissionModel<?> missionModel;

  private final SimulationFacade simulationFacade;

  //should be accessible inside problem definition
  protected final PlanningHorizon planningHorizon;

  /**
   * global constraints in the mission model, indexed by name
   */
  private final List<GlobalConstraint> globalConstraints
      = new java.util.LinkedList<>();
  /**
   * the initial seed plan to start scheduling from
   */
  private Plan initialPlan;

  /**
   * container of all goals in the problem, indexed by name
   */
  private final java.util.HashMap<String, Goal> goalsByName = new java.util.HashMap<>();

  /**
   * activity type definitions in the mission model, indexed by name
   */
  private final java.util.Map<String, ActivityType> actTypeByName
      = new java.util.HashMap<>();

  /**
   * creates a new empty problem based in the given mission model
   *
   * @param mission IN the mission model that this problem is based on
   */
  public Problem(MissionModel<?> mission, PlanningHorizon planningHorizon) {
    this.missionModel = mission;
    this.initialPlan = new PlanInMemory();
    this.planningHorizon = planningHorizon;
    //add all activity types known to aerie to scheduler index
    if( missionModel != null ) {
      for(var taskType : missionModel.getTaskSpecificationTypes().entrySet()){
        this.add(new ActivityType(taskType.getKey(), taskType.getValue()));
      }
    }
    simulationFacade = new SimulationFacade(planningHorizon, missionModel);
  }

  public SimulationFacade getSimulationFacade(){
    return simulationFacade;
  }

  /**
   * adds a new global constraint to the mission model
   *
   * @param globalConstraint IN the global constraint
   */
  public void add(GlobalConstraint globalConstraint) {
    this.globalConstraints.add(globalConstraint);
  }

  public List<GlobalConstraint> getGlobalConstraints() {
    return this.globalConstraints;
  }

  /**
   * fetches the mission model that this problem is based on
   *
   * @return the mission model that this problem is based on
   */
  public MissionModel<?> getMissionModel() {
    return missionModel;
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

  public void addAll(Collection<Goal> goals){
    for(var goal:goals){
      add(goal);
    }
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

  private void failIfActivityTypeAbsent(String name){
    if (!this.actTypeByName.containsKey(name)) {
      throw new IllegalArgumentException(
          "no activity type definition name=" + name +
          " in mission model. Either add it manually to the Problem or it should already exists in the mission model");
    }
  }

  /**
   * adds a new activity type definition to the mission model
   *
   * @param actType IN the activity type definition to add to the mission
   *     model, which must not already have a type definition with matching
   *     identifier
   */
  public void add(ActivityType actType) {

    if (actType == null) {
      throw new IllegalArgumentException(
          "adding null activity type to mission model");
    }

    final String name = actType.getName();
    if (name == null) {
      throw new IllegalArgumentException(
          "adding activity type definition with null name to mission model");
    }
    if (this.actTypeByName.containsKey(name)) {
      throw new IllegalArgumentException(
          "adding duplicate activity type definition name=" + name + " to mission model");
    }

    this.actTypeByName.put(name, actType);
  }

  public ExternalState getResource(String name){
    return simulationFacade.getResource(name);
  }

  /**
   * fetches the activity type object with the given name
   *
   * @param name IN the name associated with the requested activity type
   * @return the activity type with a matching name, or null if there is
   *     no such activity type in the mission model
   */
  public ActivityType getActivityType(String name) {
    failIfActivityTypeAbsent(name);
    return actTypeByName.get(name);
  }

  public Collection<ActivityType> getActivityTypes(){
    return Collections.unmodifiableCollection(actTypeByName.values());
  }

}
