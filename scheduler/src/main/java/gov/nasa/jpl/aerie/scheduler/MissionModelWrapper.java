package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

/**
 * contains descriptors of the mission behavior (eg activity, states, etc)
 *
 * this container provides indexes and accessors to all of the mission
 * behavior that has been encoded in a mission model codebase/jar, including
 * activity type definitions, states, and global constraints
 *
 * (similar in spirit to the raw maps currently manipulated by {@link MissionModel})
 */
//TODO: replace mission model with Merlin provided manifest
public class MissionModelWrapper {

  private final SimulationFacade simFacade;

  private final MissionModel<?> missionModel;

  /**
   * create a fresh new mission model
   *
   * the mission model will start with only the bare-bones built-in
   * epoch/state/activity/etc definitions
   */
  public MissionModelWrapper(MissionModel<?> missionModel, PlanningHorizon planningHorizon) {

    this.missionModel = missionModel;

    //TODO: change parametrization
    this.simFacade = new SimulationFacade(planningHorizon, missionModel);

    //TODO: find cleaner way to handle built-in act types

    //TODO: find cleaner way to handle windows in general
    //special activity for displaying valid windows in visualization
    add(new ActivityType("Window"));

    //include special activity type for marking plan horizon
    add(new ActivityType("HorizonMarker"));
  }

  public MissionModelWrapper() {
    this(null, null);
  }

  public ExternalState<Integer> getIntState(String name){
    return simFacade.getIntResource(name);
  }

  public ExternalState<Duration> getDurState(String name){
    return simFacade.getDurResource(name);
  }

  public ExternalState<Double> getDoubleState(String name){
    return simFacade.getDoubleResource(name);
  }
  public ExternalState<Boolean> getBoolState(String name){
    return simFacade.getBooleanResource(name);
  }

  public ExternalState<String> getStringState(String name){
    return simFacade.getStringResource(name);
  }

  public MissionModel<?> getMissionModel(){
    return missionModel;
  }

  public SimulationFacade getSimFacade(){
    return simFacade;
  }

  /**
   * adds a new state definition to the mission model
   *
   * @param stateDef IN the state definition to add to the mission model,
   *     which must not already have a state definition with matching
   *     identifier
   * @param <T> the value type of the state
   */
  public <T extends Comparable<T>> void add(StateDefinition<T> stateDef) {
  }

  public PlanningHorizon getPlanningHorizon(){
    return simFacade.getPlanningHorizon();
  }

  public SimulationFacade getSimulationFacade() {
    return simFacade;
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

  /**
   * fetches the activity type object with the given name
   *
   * @param name IN the name associated with the requested activity type
   * @return the activity type with a matching name, or null if there is
   *     no such activity type in the mission model
   */
  public ActivityType getActivityType(String name) {
    return actTypeByName.get(name);
  }

  /**
   * activity type definitions in the mission model, indexed by name
   */
  private java.util.Map<String, ActivityType> actTypeByName
      = new java.util.HashMap<>();

  /**
   * global constraints in the mission model, indexed by name
   */
  private List<GlobalConstraint> globalConstraints
      = new java.util.LinkedList<>();
}
