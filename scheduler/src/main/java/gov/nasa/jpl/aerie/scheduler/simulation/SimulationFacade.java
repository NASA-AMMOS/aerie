package gov.nasa.jpl.aerie.scheduler.simulation;

import com.google.common.collect.Maps;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.InvalidArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityInstanceId;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    if(activity.isGenerated()){
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
    } catch (TaskSpecType.UnconstructableTaskSpecException | InvalidArgumentsException e) {
      throw new SimulationException("Failed to simulate " + activity + ", possibly because it has invalid arguments", e);
    }
    insertedActivities.put(activity, serializedActivity);
  }

  /**
   * convert a simulation driver SimulationResult to a constraint evaluation engine SimulationResult
   *
   * @param driverResults the recorded results of a simulation run from the simulation driver
   * @return the same results rearranged to be suitable for use by the constraint evaluation engine
   */
  private gov.nasa.jpl.aerie.constraints.model.SimulationResults convertToConstraintModelResults(
      SimulationResults driverResults)
  {
    final var planDuration = planningHorizon.getAerieHorizonDuration();

    final var activities =  driverResults.simulatedActivities.entrySet().stream()
                                                             .map(e -> convertToConstraintModelActivityInstance(e.getKey().id(), e.getValue(), driverResults.startTime))
                                                             .collect(Collectors.toList());
    return new gov.nasa.jpl.aerie.constraints.model.SimulationResults(
        Interval.between(Duration.ZERO, planDuration),
        activities,
        Maps.transformValues(driverResults.realProfiles, this::convertToConstraintModelLinearProfile),
        Maps.transformValues(driverResults.discreteProfiles, this::convertToConstraintModelDiscreteProfile)
    );
  }

  /**
   * convert an activity entry output by the simulation driver to one suitable for the constraint evaluation engine
   *
   * @param id the name of the activity instance
   * @param driverActivity the completed activity instance details from a driver SimulationResult
   * @return an activity instance suitable for a constraint model SimulationResult
   */
  private gov.nasa.jpl.aerie.constraints.model.ActivityInstance convertToConstraintModelActivityInstance(
      long id, SimulatedActivity driverActivity, final Instant startTime)
  {
    final var startT = Duration.of(startTime.until(driverActivity.start(), ChronoUnit.MICROS), MICROSECONDS);
    final var endT = startT.plus(driverActivity.duration());
    final var activityInterval = startT.isEqualTo(endT)
        ? Interval.between(startT, endT)
        : Interval.betweenClosedOpen(startT, endT);
    return new gov.nasa.jpl.aerie.constraints.model.ActivityInstance(
        id, driverActivity.type(), driverActivity.arguments(),
        activityInterval);
  }

  /**
   * convert a linear profile output from the simulation driver to one suitable for the constraint evaluation engine
   *
   * @param driverProfile the as-simulated real profile from a driver SimulationResult
   * @return a real profile suitable for a constraint model SimulationResult, starting from the zero duration
   */
  private LinearProfile convertToConstraintModelLinearProfile(
      Pair<ValueSchema, List<Pair<Duration, RealDynamics>>> driverProfile)
  {
    final var pieces = new ArrayList<LinearProfilePiece>(driverProfile.getRight().size());
    var elapsed = Duration.ZERO;
    for (final var piece : driverProfile.getRight()) {
      final var extent = piece.getLeft();
      final var value = piece.getRight();
      pieces.add(new LinearProfilePiece(Interval.betweenClosedOpen(elapsed, elapsed.plus(extent)), value.initial, value.rate));
      elapsed = elapsed.plus(extent);
    }
    return new LinearProfile(pieces);
  }

  /**
   * convert a discrete profile output from the simulation driver to one suitable for the constraint evaluation engine
   *
   * @param driverProfile the as-simulated discrete profile from a driver SimulationResult
   * @return a discrete profile suitable for a constraint model SimulationResult, starting from the zero duration
   */
  private DiscreteProfile convertToConstraintModelDiscreteProfile(
      Pair<ValueSchema, List<Pair<Duration, SerializedValue>>> driverProfile)
  {
    final var pieces = new ArrayList<DiscreteProfilePiece>(driverProfile.getRight().size());
    var elapsed = Duration.ZERO;
    for (final var piece : driverProfile.getRight()) {
      final var extent = piece.getLeft();
      final var value = piece.getRight();
      pieces.add(new DiscreteProfilePiece(Interval.betweenClosedOpen(elapsed, elapsed.plus(extent)), value));
      elapsed = elapsed.plus(extent);
    }
    return new DiscreteProfile(pieces);
  }

  public void computeSimulationResultsUntil(Duration endTime) {
    var endTimeWithMargin = endTime;
    if(endTime.noLongerThan(Duration.MAX_VALUE.minus(MARGIN))){
      endTimeWithMargin = endTime.plus(MARGIN);
    }
    var results = driver.getSimulationResultsUpTo(endTimeWithMargin);
    //compare references
    if(results != lastSimDriverResults) {
      //simulation results from the last simulation, as converted for use by the constraint evaluation engine
      lastSimConstraintResults = convertToConstraintModelResults(results);
      lastSimDriverResults = results;
    }
  }

  public Duration getCurrentSimulationEndTime(){
    return driver.getCurrentSimulationEndTime();
  }
}
