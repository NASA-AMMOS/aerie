package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityInstanceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

/**
 * A facade for simulating plans and processing simulation results.
 */
@SuppressWarnings("UnnecessaryToStringCall")
public class SimulationFacade {

  private static final Logger logger = LoggerFactory.getLogger(SimulationFacade.class);

  private final MissionModel<?> missionModel;

  // planning horizon
  private final PlanningHorizon planningHorizon;
  private Map<String, ActivityType> activityTypes;
  private IncrementalSimulationDriver<?> driver;
  private int itSimActivityId;

  //simulation results from the last simulation, as output directly by simulation driver
  private SimulationResults lastSimDriverResults;
  private gov.nasa.jpl.aerie.constraints.model.SimulationResults lastSimConstraintResults;
  private final Map<SchedulingActivityInstanceId, ActivityInstanceId> planActInstanceIdToSimulationActInstanceId = new HashMap<>();
  private final Map<ActivityInstance, SerializedActivity> insertedActivities;
  private static final Duration MARGIN = Duration.of(5, MICROSECONDS);


  public gov.nasa.jpl.aerie.constraints.model.SimulationResults getLatestConstraintSimulationResults(){
    return lastSimConstraintResults;
  }

  public SimulationFacade(PlanningHorizon planningHorizon, MissionModel<?> missionModel) {
    this.missionModel = missionModel;
    this.planningHorizon = planningHorizon;
    this.driver = new IncrementalSimulationDriver<>(missionModel);
    this.itSimActivityId = 0;
    this.insertedActivities = new HashMap<>();
    this.activityTypes = new HashMap<>();
  }

  public void setActivityTypes(Collection<ActivityType> activityTypes){
    this.activityTypes = new HashMap<>();
    activityTypes.forEach(at -> this.activityTypes.put(at.getName(), at));
  }

  /**
   * Fetches activity instance durations from last simulation
   *
   * @param activityInstance the activity instance we want the duration for
   * @return the duration if found in the last simulation, null otherwise
   */
  public Optional<Duration> getActivityDuration(ActivityInstance activityInstance) {
    if (!insertedActivities.containsKey(activityInstance)) return Optional.empty();
    if(!planActInstanceIdToSimulationActInstanceId.containsKey(activityInstance.getId())){
      logger.error("You need to simulate before requesting activity duration");
      return Optional.empty();
    }
    var duration = driver.getActivityDuration(planActInstanceIdToSimulationActInstanceId.get(activityInstance.getId()));
    if(duration.isEmpty()){
      logger.error("Incremental simulation is probably outdated, check that no activity is removed between simulation and querying");
    }
    return duration;
  }

  private ActivityInstanceId getIdOfRootParent(SimulationResults results, ActivityInstanceId instanceId){
    final var act = results.simulatedActivities.get(instanceId);
    if (act != null) {
      if (act.parentId() == null) {
        return instanceId;
      } else {
        return getIdOfRootParent(results, act.parentId());
      }
    } else {
      final var unfinishedAct = Objects.requireNonNull(results.unfinishedActivities.get(instanceId));
      if (unfinishedAct.parentId() == null) {
        return instanceId;
      } else {
        return getIdOfRootParent(results, unfinishedAct.parentId());
      }
    }
  }

  public Map<ActivityInstance, SchedulingActivityInstanceId> getAllGeneratedActivities(final Plan plan, final Duration endTime){
    computeSimulationResultsUntil(plan, endTime);
    Map<ActivityInstance, SchedulingActivityInstanceId> generatedActivities = new HashMap<>();
    this.lastSimDriverResults.simulatedActivities.forEach( (activityInstanceId, activity) -> {
      final var rootParent = getIdOfRootParent(this.lastSimDriverResults, activityInstanceId);
      if(!rootParent.equals(activityInstanceId)){
        //generated activity
        final var schedulingActId = planActInstanceIdToSimulationActInstanceId.entrySet().stream().filter(
            entry -> entry.getValue().equals(rootParent)
        ).findFirst().get().getKey();
        final var activityInstance = ActivityInstance.of(activityTypes.get(activity.type()),
                                                          this.planningHorizon.toDur(activity.start()),
                                                          activity.duration(),
                                                          activity.arguments(),
                                                          schedulingActId);
        generatedActivities.put(activityInstance, schedulingActId);
      }
    });
    return generatedActivities;
  }

  public void invalidateResultsAfter(final Duration cutoff) {
    this.lastSimDriverResults = null;
    this.lastSimConstraintResults = null;
  }

  public void removeActivitiesFromSimulation(final Plan plan, final Collection<ActivityInstance> activities) throws SimulationException {
    for(final var act: activities){
      if(insertedActivities.containsKey(act)){
        insertedActivities.remove(act);
      } else {
        throw new AssertionError("Cannot remove activities if they were never added! " + activities + " " + insertedActivities);
      }
    }
    //reset incremental simulation
    final var oldInsertedActivities = new HashMap<>(insertedActivities);
    insertedActivities.clear();
    planActInstanceIdToSimulationActInstanceId.clear();
    driver.initSimulation();
    simulateActivities(plan, oldInsertedActivities.keySet());
  }

  /**
   * Replaces an activity instance with another, strictly when they have the same id
   * @param toBeReplaced the activity to be replaced
   * @param replacement the replacement activity
   */
  public void replaceActivityFromSimulation(final ActivityInstance toBeReplaced, final ActivityInstance replacement){
    if(toBeReplaced.type() != replacement.type()||
       toBeReplaced.startTime() != replacement.startTime()||
       !(toBeReplaced.arguments().equals(replacement.arguments()))) {
      throw new IllegalArgumentException("When replacing an activity, you can only update the duration");
    }
    if(!insertedActivities.containsKey(toBeReplaced)){
      throw new IllegalArgumentException("Trying to replace an activity that has not been previously simulated");
    }
    final var associated = insertedActivities.get(toBeReplaced);
    insertedActivities.remove(toBeReplaced);
    insertedActivities.put(replacement, associated);
    final var simulationId = this.planActInstanceIdToSimulationActInstanceId.get(toBeReplaced.id());
    this.planActInstanceIdToSimulationActInstanceId.remove(toBeReplaced.id());
    this.planActInstanceIdToSimulationActInstanceId.put(replacement.id(), simulationId);
  }

  public void simulateActivities(final Plan plan, final Collection<ActivityInstance> activities)
  throws SimulationException {
    for (final var act : plan.getActivitiesById().entrySet()) {
      planActInstanceIdToSimulationActInstanceId.computeIfAbsent(act.getKey(), $ -> new ActivityInstanceId(itSimActivityId++));
    }
    final var newSchedule = Schedule.of(planActInstanceIdToSimulationActInstanceId, plan);
    for (final var activity : activities) {
      if (!newSchedule.contains(new StartTime.OffsetFromPlanStart(activity.startTime()), new SerializedActivity(activity.getType().getName(), activity.arguments()))) {
        throw new AssertionError("Plan must contain all activities:" + activity + " " + newSchedule);
      }
    }
    final var activitiesSortedByStartTime =
        activities.stream().sorted(Comparator.comparing(ActivityInstance::startTime)).toList();
    for (final var activityInstance : activitiesSortedByStartTime) {
      try {
        simulateActivity(plan, activityInstance);
      } catch (SimulationException e) {
        throw new SimulationException("Failed to instantiate "
                                      + activityInstance
                                      + ". Consider checking that its arguments are valid.", e);
      }
    }

  }

  public static class SimulationException extends Exception {
    SimulationException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  public void simulateActivity(final Plan plan, final ActivityInstance activity) throws SimulationException {
    if(activity.getParentActivity().isPresent()) {
      throw new Error("This method should not be called with a generated activity but with its top-level parent.");
    }
    final var arguments = new HashMap<>(activity.arguments());
    if (activity.duration() != null) {
      final var durationType = activity.getType().getDurationType();
      if (durationType instanceof DurationType.Controllable dt) {
        arguments.put(dt.parameterName(), SerializedValue.of(activity.duration().in(Duration.MICROSECONDS)));
      } else if (durationType instanceof DurationType.Uncontrollable) {
        // If an activity has already been simulated, it will have a duration, even if its DurationType is Uncontrollable.
      } else {
        throw new Error("Unhandled variant of DurationType: " + durationType);
      }
    } else {
      logger.warn("Activity has unconstrained duration {}", activity);
    }
    var activityIdSim = new ActivityInstanceId(itSimActivityId++);
    planActInstanceIdToSimulationActInstanceId.put(activity.getId(), activityIdSim);

    var serializedActivity = new SerializedActivity(activity.getType().getName(), arguments);

    var newSchedule = Schedule.of(planActInstanceIdToSimulationActInstanceId, plan);
    newSchedule = newSchedule.replace(planActInstanceIdToSimulationActInstanceId.get(activity.getId()), new StartTime.OffsetFromPlanStart(activity.startTime()), serializedActivity);
    try {
      driver.simulateActivity(newSchedule, activityIdSim);
    } catch (InstantiationException e) {
      throw new SimulationException("Failed to simulate " + activity + ", possibly because it has invalid arguments", e);
    }
    insertedActivities.put(activity, serializedActivity);
  }

  public void computeSimulationResultsUntil(final Plan plan, final Duration endTime) {
    var endTimeWithMargin = endTime;
    if(endTime.noLongerThan(Duration.MAX_VALUE.minus(MARGIN))){
      endTimeWithMargin = endTime.plus(MARGIN);
    }
    for (final var act : plan.getActivitiesById().entrySet()) {
      planActInstanceIdToSimulationActInstanceId.computeIfAbsent(act.getKey(), $ -> new ActivityInstanceId(itSimActivityId++));
    }
    final var results = driver.getSimulationResultsUpTo(Schedule.of(planActInstanceIdToSimulationActInstanceId, plan), this.planningHorizon.getStartInstant(), endTimeWithMargin);
    //compare references
    if(results != lastSimDriverResults) {
      //simulation results from the last simulation, as converted for use by the constraint evaluation engine
      lastSimConstraintResults = SimulationResultsConverter.convertToConstraintModelResults(results, planningHorizon.getAerieHorizonDuration());
      lastSimDriverResults = results;
    }
  }

  public Duration getCurrentSimulationEndTime(){
    return driver.getCurrentSimulationEndTime();
  }
}
