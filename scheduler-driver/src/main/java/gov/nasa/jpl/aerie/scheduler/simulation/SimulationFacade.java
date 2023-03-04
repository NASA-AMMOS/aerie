package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivityId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
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
public class SimulationFacade {

  private static final Logger logger = LoggerFactory.getLogger(SimulationFacade.class);

  private final MissionModel<?> missionModel;

  // planning horizon
  private final PlanningHorizon planningHorizon;
  private final boolean useResourceTracker;
  private Map<String, ActivityType> activityTypes;
  private ResumableSimulationDriver<?> driver;
  private int itSimActivityId;

  //simulation results from the last simulation, as output directly by simulation driver
  private SimulationResults lastSimDriverResults;
  private gov.nasa.jpl.aerie.constraints.model.SimulationResults lastSimConstraintResults;
  private final Map<SchedulingActivityDirectiveId, ActivityDirectiveId>
      planActDirectiveIdToSimulationActivityDirectiveId = new HashMap<>();
  private final Map<SchedulingActivityDirective, ActivityDirective> insertedActivities;
  private static final Duration MARGIN = Duration.of(5, MICROSECONDS);

  public gov.nasa.jpl.aerie.constraints.model.SimulationResults getLatestConstraintSimulationResults(){
    return lastSimConstraintResults;
  }

  public SimulationFacade(final PlanningHorizon planningHorizon, final MissionModel<?> missionModel) {
    this(planningHorizon, missionModel, false);
  }

  public SimulationFacade(final PlanningHorizon planningHorizon, final MissionModel<?> missionModel, final boolean useResourceTracker) {
    this.useResourceTracker = useResourceTracker;
    this.missionModel = missionModel;
    this.planningHorizon = planningHorizon;
    this.driver = new ResumableSimulationDriver<>(missionModel, planningHorizon.getAerieHorizonDuration(), useResourceTracker);
    this.itSimActivityId = 0;
    this.insertedActivities = new HashMap<>();
    this.activityTypes = new HashMap<>();
  }

  public void setActivityTypes(final Collection<ActivityType> activityTypes){
    this.activityTypes = new HashMap<>();
    activityTypes.forEach(at -> this.activityTypes.put(at.getName(), at));
  }

  /**
   * Fetches activity instance durations from last simulation
   *
   * @param schedulingActivityDirective the activity instance we want the duration for
   * @return the duration if found in the last simulation, null otherwise
   */
  public Optional<Duration> getActivityDuration(final SchedulingActivityDirective schedulingActivityDirective) {
    if(!planActDirectiveIdToSimulationActivityDirectiveId.containsKey(schedulingActivityDirective.getId())){
      logger.error("You need to simulate before requesting activity duration");
      return Optional.empty();
    }
    final var duration = driver.getActivityDuration(planActDirectiveIdToSimulationActivityDirectiveId.get(
        schedulingActivityDirective.getId()));
    if(duration.isEmpty()){
      logger.error("Simulation is probably outdated, check that no activity is removed between simulation and querying");
    }
    return duration;
  }

  private ActivityDirectiveId getIdOfRootParent(SimulationResults results, SimulatedActivityId instanceId){
    final var act = results.simulatedActivities.get(instanceId);
    if(act.parentId() == null){
      // SAFETY: any activity that has no parent must have a directive id.
      return act.directiveId().get();
    } else {
      return getIdOfRootParent(results, act.parentId());
    }
  }

  public Map<SchedulingActivityDirective, SchedulingActivityDirectiveId> getAllChildActivities(final Duration endTime){
    computeSimulationResultsUntil(endTime);
    final Map<SchedulingActivityDirective, SchedulingActivityDirectiveId> childActivities = new HashMap<>();
    this.lastSimDriverResults.simulatedActivities.forEach( (activityInstanceId, activity) -> {
      if (activity.parentId() == null) return;
      final var rootParent = getIdOfRootParent(this.lastSimDriverResults, activityInstanceId);
      final var schedulingActId = planActDirectiveIdToSimulationActivityDirectiveId.entrySet().stream().filter(
          entry -> entry.getValue().equals(rootParent)
      ).findFirst().get().getKey();
      final var activityInstance = SchedulingActivityDirective.of(
          activityTypes.get(activity.type()),
          this.planningHorizon.toDur(activity.start()),
          activity.duration(),
          activity.arguments(),
          schedulingActId,
          null,
          true);
      childActivities.put(activityInstance, schedulingActId);
    });
    return childActivities;
  }

  public void removeActivitiesFromSimulation(final Collection<SchedulingActivityDirective> activities) throws SimulationException {
    var atLeastOne = false;
    for(final var act: activities){
      if(insertedActivities.containsKey(act)){
        atLeastOne = true;
        insertedActivities.remove(act);
      }
    }
    //reset resumable simulation
    if(atLeastOne){
      final var oldInsertedActivities = new HashMap<>(insertedActivities);
      insertedActivities.clear();
      planActDirectiveIdToSimulationActivityDirectiveId.clear();
      driver = new ResumableSimulationDriver<>(missionModel, planningHorizon.getAerieHorizonDuration(), useResourceTracker);
      simulateActivities(oldInsertedActivities.keySet());
    }
  }

  /**
   * Replaces an activity instance with another, strictly when they have the same id
   * @param toBeReplaced the activity to be replaced
   * @param replacement the replacement activity
   */
  public void replaceActivityFromSimulation(final SchedulingActivityDirective toBeReplaced, final SchedulingActivityDirective replacement){
    if(toBeReplaced.type() != replacement.type()||
       toBeReplaced.startOffset() != replacement.startOffset()||
       !(toBeReplaced.arguments().equals(replacement.arguments()))) {
      throw new IllegalArgumentException("When replacing an activity, you can only update the duration");
    }
    if(!insertedActivities.containsKey(toBeReplaced)){
      throw new IllegalArgumentException("Trying to replace an activity that has not been previously simulated");
    }
    final var associated = insertedActivities.get(toBeReplaced);
    insertedActivities.remove(toBeReplaced);
    insertedActivities.put(replacement, associated);
    final var simulationId = this.planActDirectiveIdToSimulationActivityDirectiveId.get(toBeReplaced.id());
    this.planActDirectiveIdToSimulationActivityDirectiveId.remove(toBeReplaced.id());
    this.planActDirectiveIdToSimulationActivityDirectiveId.put(replacement.id(), simulationId);
  }

  public void simulateActivities(final Collection<SchedulingActivityDirective> activities) {
    final var activitiesSortedByStartTime =
        activities.stream().sorted(Comparator.comparing(SchedulingActivityDirective::startOffset)).toList();
    final Map<ActivityDirectiveId, ActivityDirective> directivesToSimulate = new HashMap<>();

    for(final var activity : activitiesSortedByStartTime){
      final var activityIdSim = new ActivityDirectiveId(itSimActivityId++);
      planActDirectiveIdToSimulationActivityDirectiveId.put(activity.getId(), activityIdSim);
    }

    for(final var activity : activitiesSortedByStartTime) {
      final var activityDirective = schedulingActToActivityDir(activity);
      directivesToSimulate.put(
          planActDirectiveIdToSimulationActivityDirectiveId.get(activity.getId()),
          activityDirective);
      insertedActivities.put(activity, activityDirective);
    }
    driver.simulateActivities(directivesToSimulate);
  }

  public static class SimulationException extends Exception {
    SimulationException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  public void simulateActivity(final SchedulingActivityDirective activity) throws SimulationException {
    final var activityIdSim = new ActivityDirectiveId(itSimActivityId++);
    final var activityDirective = schedulingActToActivityDir(activity);

    planActDirectiveIdToSimulationActivityDirectiveId.put(activity.getId(), activityIdSim);
    driver.simulateActivity(activityDirective, activityIdSim);
    insertedActivities.put(activity, activityDirective);
  }

  public void computeSimulationResultsUntil(final Duration endTime) {
    var endTimeWithMargin = endTime;
    if(endTime.noLongerThan(Duration.MAX_VALUE.minus(MARGIN))){
      endTimeWithMargin = endTime.plus(MARGIN);
    }
    final var results = driver.getSimulationResultsUpTo(this.planningHorizon.getStartInstant(), endTimeWithMargin);
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

  private ActivityDirective schedulingActToActivityDir(SchedulingActivityDirective activity) {
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
    final var serializedActivity = new SerializedActivity(activity.getType().getName(), arguments);
    return new ActivityDirective(
        activity.startOffset(),
        serializedActivity,
        planActDirectiveIdToSimulationActivityDirectiveId.get(activity.anchorId()),
        activity.anchoredToStart());
  }
}
