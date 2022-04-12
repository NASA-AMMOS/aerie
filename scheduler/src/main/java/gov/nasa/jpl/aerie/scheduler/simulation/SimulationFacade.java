package gov.nasa.jpl.aerie.scheduler.simulation;

import com.google.common.collect.Maps;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityInstanceId;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Includes : (1) providing resulting resource values to scheduler constructs
 * (2) providing durations of activity instances
 */
@SuppressWarnings("UnnecessaryToStringCall")
public class SimulationFacade {

  private static final Logger logger = LoggerFactory.getLogger(SimulationFacade.class);

  // Resource feeders, mapping resource names to their corresponding resource accessor resulting from simulation results
  private final Map<String, SimResource> resources;

  private final MissionModel<?> missionModel;

  // planning horizon
  private final PlanningHorizon planningHorizon;
  private IncrementalSimulationDriver driver;
  private int itSimActivityId;

  //simulation results from the last simulation, as output directly by simulation driver
  private SimulationResults lastSimDriverResults;

  private final Map<SchedulingActivityInstanceId, ActivityInstanceId> planActInstanceIdToSimulationActInstanceId = new HashMap<>();

  private final Map<ActivityInstance, SerializedActivity> insertedActivities;

  private static final Duration MARGIN = Duration.of(5,MICROSECONDS);

  /**
   * Accessor for integer resource feeders
   *
   * @param resourceName the name of the resource
   * @return the resource feeder if it exists, null otherwise
   */
  public SimResource getResource(String resourceName) {
    if (!resources.containsKey(resourceName)) {
      resources.put(resourceName, new SimResource(this));
    }
    return resources.get(resourceName);
  }

  public SimulationFacade(PlanningHorizon planningHorizon, MissionModel<?> missionModel) {
    this.missionModel = missionModel;
    this.planningHorizon = planningHorizon;
    this.resources = new HashMap<>();
    this.driver = new IncrementalSimulationDriver(missionModel);
    this.itSimActivityId = 0;
    this.insertedActivities = new HashMap<>();
  }

  public PlanningHorizon getPlanningHorizon(){
    return this.planningHorizon;
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
      driver = new IncrementalSimulationDriver(missionModel);
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

    final var arguments = new HashMap<>(activity.getArguments());
    if (activity.hasDuration()) {
      final var durationType = activity.getType().getDurationType();
      if (durationType instanceof DurationType.Controllable dt) {
        arguments.put(dt.parameterName(), new DurationValueMapper().serializeValue(activity.getDuration()));
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
    } catch (TaskSpecType.UnconstructableTaskSpecException e) {
      throw new SimulationException("Failed to simulate " + activity + ", possibly because it has invalid arguments", e);
    }
    insertedActivities.put(activity, serializedActivity);
  }

  /**
   * Fetches the resource schemas from the mission model
   *
   * @return a map from resource name to valueschema
   */
  private Map<String, ValueSchema> getResourceSchemas() {
    final var schemas = new HashMap<String, ValueSchema>();

    for (final var entry : this.missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      schemas.put(name, resource.getSchema());
    }

    return schemas;
  }

  /**
   * Updates resource feeders with results from a simulation.
   *
   * @param results results generated from a simulation driver run
   */
  private void handleSimulationResults(SimulationResults results) {
    //simulation results from the last simulation, as converted for use by the constraint evaluation engine
    final var lastConstraintModelResults = convertToConstraintModelResults(
        results);

    final var sc = getResourceSchemas();
    // maps resource names to their local type

    for (final var schema : sc.entrySet()) {
      if (!resources.containsKey(schema.getKey())) {
        resources.put(schema.getKey(), new SimResource(this));
      }
    }

    for (final var entry : results.resourceSamples.entrySet()) {
      final var name = entry.getKey();
      getResource(name).initFromSimRes(
          name,
          lastConstraintModelResults,
          entry.getValue(),
          this.planningHorizon.getStartAerie());
    }
    lastSimDriverResults = results;
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

    return new gov.nasa.jpl.aerie.constraints.model.SimulationResults(
        Window.between(Duration.ZERO, planDuration),
        driverResults.simulatedActivities.entrySet().stream()
                                         .map(e -> convertToConstraintModelActivityInstance(e.getKey().id(), e.getValue()))
                                         .collect(Collectors.toList()),
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
      long id, SimulatedActivity driverActivity)
  {
    final var planStartT = this.planningHorizon.getStartHuginn().toInstant();
    final var startT = Duration.of(planStartT.until(driverActivity.start, ChronoUnit.MICROS), MICROSECONDS);
    final var endT = startT.plus(driverActivity.duration);
    return new gov.nasa.jpl.aerie.constraints.model.ActivityInstance(
        id, driverActivity.type, driverActivity.arguments,
        Window.betweenClosedOpen(startT, endT));
  }

  /**
   * convert a linear profile output from the simulation driver to one suitable for the constraint evaluation engine
   *
   * @param driverProfile the as-simulated real profile from a driver SimulationResult
   * @return a real profile suitable for a constraint model SimulationResult, starting from the zero duration
   */
  private LinearProfile convertToConstraintModelLinearProfile(
      List<Pair<Duration, RealDynamics>> driverProfile)
  {
    final var pieces = new ArrayList<LinearProfilePiece>(driverProfile.size());
    var elapsed = Duration.ZERO;
    for (final var piece : driverProfile) {
      final var extent = piece.getLeft();
      final var value = piece.getRight();
      pieces.add(new LinearProfilePiece(Window.between(elapsed, elapsed.plus(extent)), value.initial, value.rate));
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
      pieces.add(new DiscreteProfilePiece(Window.between(elapsed, elapsed.plus(extent)), value));
      elapsed = elapsed.plus(extent);
    }
    return new DiscreteProfile(pieces);
  }

  public void updateResourcesIfNecessary(Duration endTime) {
    var endTimeWithMargin = endTime;
    if(endTime.noLongerThan(Duration.MAX_VALUE.minus(MARGIN))){
      endTimeWithMargin = endTime.plus(MARGIN);
    }
    var results = driver.getSimulationResultsUpTo(endTimeWithMargin);
    //compare references
    if(results != lastSimDriverResults) {
      handleSimulationResults(results);
    }
  }
}
