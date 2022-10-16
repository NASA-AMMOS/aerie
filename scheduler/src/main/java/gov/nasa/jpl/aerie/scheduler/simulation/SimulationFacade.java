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
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityInstanceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
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
    if(act.parentId() == null){
      return instanceId;
    } else {
      return getIdOfRootParent(results, act.parentId());
    }
  }

  public Map<ActivityInstance, SchedulingActivityInstanceId> getAllGeneratedActivities(Duration endTime){
    computeSimulationResultsUntil(endTime);
    Map<ActivityInstance, SchedulingActivityInstanceId> generatedActivities = new HashMap<>();
    this.lastSimDriverResults.simulatedActivities.forEach( (activityInstanceId, activity) -> {
      final var rootParent = getIdOfRootParent(this.lastSimDriverResults, activityInstanceId);
      if(!rootParent.equals(activityInstanceId)){
        //generated activity
        final var schedulingActId = planActInstanceIdToSimulationActInstanceId.entrySet().stream().filter(
            entry -> entry.getValue().equals(rootParent)
        ).findFirst().get().getKey();
        final var activityInstance = new ActivityInstance(activityTypes.get(activity.type()),
                                                          this.planningHorizon.toDur(activity.start()),
                                                          activity.duration(),
                                                          activity.arguments(),
                                                          schedulingActId);
        generatedActivities.put(activityInstance, schedulingActId);
      }
    });
    return generatedActivities;
  }

  public void removeActivitiesFromSimulation(final Collection<ActivityInstance> activities) throws SimulationException {
    var atLeastOne = false;
    for(var act: activities){
      if(insertedActivities.containsKey(act)){
        atLeastOne = true;
        insertedActivities.remove(act);
      }
    }
    //reset incremental simulation
    if(atLeastOne){
      final var oldInsertedActivities = new HashMap<>(insertedActivities);
      insertedActivities.clear();
      planActInstanceIdToSimulationActInstanceId.clear();
      driver = new IncrementalSimulationDriver<>(missionModel);
      simulateActivities(oldInsertedActivities.keySet());
    }
  }

  public void simulateActivities(final Collection<ActivityInstance> activities)
  throws SimulationException {
    final var activitiesSortedByStartTime =
        activities.stream().sorted(Comparator.comparing(ActivityInstance::getStartTime)).toList();
    for (final var activityInstance : activitiesSortedByStartTime) {
      try {
        simulateActivity(activityInstance);
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

  public void simulateActivity(final ActivityInstance activity) throws SimulationException {
    if(activity.getParentActivity().isPresent()) {
      throw new Error("This method should not be called with a generated activity but with its top-level parent.");
    }
    final var arguments = new HashMap<>(activity.getArguments());
    if (activity.hasDuration()) {
      final var durationType = activity.getType().getDurationType();
      if (durationType instanceof DurationType.Controllable dt) {
        arguments.put(dt.parameterName(), SerializedValue.of(activity.getDuration().in(Duration.MICROSECONDS)));
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

    try {
      driver.simulateActivity(serializedActivity, activity.getStartTime(), activityIdSim);
    } catch (InstantiationException e) {
      throw new SimulationException("Failed to simulate " + activity + ", possibly because it has invalid arguments", e);
    }
    insertedActivities.put(activity, serializedActivity);
  }

  public void computeSimulationResultsUntil(Duration endTime) {
    var endTimeWithMargin = endTime;
    if(endTime.noLongerThan(Duration.MAX_VALUE.minus(MARGIN))){
      endTimeWithMargin = endTime.plus(MARGIN);
    }
    var results = driver.getSimulationResultsUpTo(this.planningHorizon.getStartInstant(), endTimeWithMargin);
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
