package gov.nasa.jpl.aerie.scheduler;

import com.google.common.collect.Maps;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

/**
 * A facade for simulating plans and processing simulation results.
 * Includes : (1) providing resulting resource values to scheduler constructs
 * (2) providing durations of activity instances
 */
public class SimulationFacade {

  // Resource feeders, mapping resource names to their corresponding resource accessor resulting from simulation results
  private final Map<String, SimResource<Integer>> feedersInt;
  private final Map<String, SimResource<Double>> feedersDouble;
  private final Map<String, SimResource<Boolean>> feedersBool;
  private final Map<String, SimResource<String>> feedersString;
  private final Map<String, SimResource<Duration>> feedersDuration;

  private final MissionModel<?> missionModel;

  // planning horizon
  private final PlanningHorizon planningHorizon;

  // local types for resources
  final private static String STRING = "String";
  final private static String INTEGER = "Integer";
  final private static String DOUBLE = "Double";
  final private static String BOOLEAN = "Boolean";
  final private static String DURATION = "Duration";

  // maps resource names to their local type
  private Map<String, String> nameToType;

  // stores the names of resources unsupported by the simulation facade due to type conversion
  private final Set<String> unsupportedResources = new HashSet<>();

  //simulation results from the last simulation, as output directly by simulation driver
  private SimulationResults lastSimDriverResults;

  /**
   * Accessor for integer resource feeders
   *
   * @param resourceName the name of the resource
   * @return the resource feeder if it exists, null otherwise
   */
  public SimResource<Integer> getIntResource(String resourceName) {
    if (!feedersInt.containsKey(resourceName)) {
      feedersInt.put(resourceName, new SimResource<>());
    }
    return feedersInt.get(resourceName);
  }

  public SimResource<Duration> getDurResource(String resourceName) {
    if (!feedersDuration.containsKey(resourceName)) {
      feedersDuration.put(resourceName, new SimResource<>());
    }
    return feedersDuration.get(resourceName);
  }

  /**
   * Accessor for double resource feeders
   *
   * @param resourceName the name of the resource
   * @return the resource feeder if it exists, null otherwise
   */
  public SimResource<Double> getDoubleResource(String resourceName) {
    if (!feedersDouble.containsKey(resourceName)) {
      feedersDouble.put(resourceName, new SimResource<>());
    }
    return feedersDouble.get(resourceName);
  }

  /**
   * Accessor for string resource feeders
   *
   * @param resourceName the name of the resource
   * @return the resource feeder if it exists, null otherwise
   */
  public SimResource<String> getStringResource(String resourceName) {
    if (!feedersString.containsKey(resourceName)) {
      feedersString.put(resourceName, new SimResource<>());
    }
    return feedersString.get(resourceName);
  }

  /**
   * Accessor for boolean resource feeders
   *
   * @param resourceName the name of the resource
   * @return the resource feeder if it exists, null otherwise
   */
  public SimResource<Boolean> getBooleanResource(String resourceName) {
    if (!feedersBool.containsKey(resourceName)) {
      feedersBool.put(resourceName, new SimResource<>());
    }
    return feedersBool.get(resourceName);
  }

  public SimulationFacade(PlanningHorizon planningHorizon, MissionModel<?> missionModel) {
    this.missionModel = missionModel;
    this.planningHorizon = planningHorizon;
    feedersInt = new HashMap<>();
    feedersDouble = new HashMap<>();
    feedersBool = new HashMap<>();
    feedersString = new HashMap<>();
    feedersDuration = new HashMap<>();
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
  public Duration getActivityDuration(ActivityInstance activityInstance) {
    if (lastSimDriverResults == null) {
      System.out.println("You need to simulate before requesting activity duration");
    }
    final var simAct = lastSimDriverResults.simulatedActivities.get(activityInstance.getName());
    if (simAct != null) {
      return simAct.duration;
    } else {
      if(lastSimDriverResults.unfinishedActivities.get(activityInstance.getName()) != null){
        System.out.println("Activity "
                           + activityInstance.getName()
                           + " has not finished, check planning horizon ?");
      } else{
        System.out.println("Simulation has been launched but activity with name= "
                           + activityInstance.getName()
                           + " has not been found");
      }
    }
    return null;
  }

  /**
   * Simulates schedule and processes results to produce accessors for resources and activities. The complete set of
   * activities scheduled so
   * far should be passed for every simulation. Simulation runs for the whole planning horizon.
   *
   * @param plan the plan to simulate
   */
  public void simulatePlan(Plan plan) {

    final var actsInPlan = plan.getActivitiesByTime();
    final var schedule = new HashMap<String, Pair<Duration, SerializedActivity>>();

    for (final var act : actsInPlan) {

      Map<String, SerializedValue> params = new HashMap<>();
      act.getParameters().forEach((name, value) -> params.put(name, this.serialize(value)));
      if(act.getDuration()!= null) {
        params.put("duration", this.serialize(act.getDuration()));
      } else{
        System.out.println("Warning : activity has no duration parameter");
      }
      schedule.put(act.getName(), Pair.of(
          act.getStartTime(),
          new SerializedActivity(act.getType().getName(), params)));
    }

    final var simulationDuration = planningHorizon.getAerieHorizonDuration();

    final var results = SimulationDriver.simulate(
        this.missionModel,
        schedule,
        Instant.now(),
        simulationDuration);

    handleSimulationResults(results);

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
    this.lastSimDriverResults = results;
    //simulation results from the last simulation, as converted for use by the constraint evaluation engine
    final var lastConstraintModelResults = convertToConstraintModelResults(
        results);

    final var sc = getResourceSchemas();
    nameToType = new HashMap<>();

    for (final var schema : sc.entrySet()) {
      schema.getValue().match(new ValueSchema.Visitor<String>() {

        @Override
        public String onReal() {
          final var nameRes = schema.getKey();
          nameToType.put(nameRes, DOUBLE);
          if (!feedersDouble.containsKey(nameRes)) {
            feedersDouble.put(nameRes, new SimResource<>());
          }
          return DOUBLE;
        }

        @Override
        public String onInt() {
          final var nameRes = schema.getKey();
          nameToType.put(nameRes, INTEGER);
          if (!feedersInt.containsKey(nameRes)) {
            feedersInt.put(nameRes, new SimResource<>());
          }
          return INTEGER;
        }

        @Override
        public String onBoolean() {
          final var nameRes = schema.getKey();
          nameToType.put(nameRes, BOOLEAN);
          if (!feedersBool.containsKey(nameRes)) {
            feedersBool.put(nameRes, new SimResource<>());
          }
          return BOOLEAN;
        }

        @Override
        public String onString() {
          final var nameRes = schema.getKey();
          //System.out.println("String = " + nameRes);
          nameToType.put(nameRes, STRING);
          if (feedersString.containsKey(nameRes)) {
            feedersString.put(nameRes, new SimResource<>());
          }
          return STRING;
        }

        @Override
        public String onDuration() {
          final var nameRes = schema.getKey();
          nameToType.put(nameRes, DURATION);
          if (!feedersDuration.containsKey(nameRes)) {
            feedersDuration.put(nameRes, new SimResource<>());
          }
          return DURATION;
        }

        @Override
        public String onPath() {
          final var nameRes = schema.getKey();
          nameToType.put(nameRes, STRING);
          if (!feedersString.containsKey(nameRes)) {
            feedersString.put(nameRes, new SimResource<>());
          }
          return STRING;
        }

        @Override
        public String onSeries(ValueSchema value) {
          //System.out.println("Series = " + schema.getKey());
          unsupportedResources.add(schema.getKey());
          return "Other";
        }

        @Override
        public String onStruct(Map<String, ValueSchema> value) {
          //System.out.println("Struct = " + schema.getKey());
          unsupportedResources.add(schema.getKey());
          return "Other";
        }

        @Override
        public String onVariant(List<ValueSchema.Variant> variants) {
          final var nameRes = schema.getKey();
          //System.out.println("Variant " + nameRes);
          nameToType.put(nameRes, STRING);
          if (!feedersString.containsKey(nameRes)) {
            feedersString.put(nameRes, new SimResource<>());
          }
          return "String";
        }
      });

    }
    for (final var entry : results.resourceSamples.entrySet()) {
      final var name = entry.getKey();
      final var type = nameToType.get(entry.getKey());
      if (!unsupportedResources.contains(name)) {
        switch (type) {
          case DURATION -> getDurResource(name).initFromSimRes(
              name,
              new DurationValueMapper(),
              lastConstraintModelResults,
              deserialize(entry.getValue(), new DurationValueMapper()),
              this.planningHorizon.getStartAerie());
          case INTEGER -> getIntResource(name).initFromSimRes(
              name,
              new IntegerValueMapper(),
              lastConstraintModelResults,
              deserialize(entry.getValue(), new IntegerValueMapper()),
              this.planningHorizon.getStartAerie());
          case BOOLEAN -> getBooleanResource(name).initFromSimRes(
              name,
              new BooleanValueMapper(),
              lastConstraintModelResults,
              deserialize(
                  entry.getValue(),
                  new BooleanValueMapper()),
              this.planningHorizon.getStartAerie());
          case DOUBLE -> getDoubleResource(name).initFromSimRes(
              name,
              new DoubleValueMapper(),
              lastConstraintModelResults,
              deserialize(entry.getValue(), new DoubleValueMapper()),
              this.planningHorizon.getStartAerie());
          case STRING -> getStringResource(name).initFromSimRes(
              name,
              new StringValueMapper(),
              lastConstraintModelResults,
              deserialize(entry.getValue(), new StringValueMapper()),
              this.planningHorizon.getStartAerie());
          default -> throw new IllegalArgumentException("Not supported");
        }
      }
    }
  }

  /**
   * convert a simulation driver SimulationResult to a constraint evaluation engine SimulationResult
   *
   * @param driverResults the recorded results of a simulation run from the simulation driver
   * @return the same results rearranged to be suitable for use by the constraint evaluation engine
   */
  gov.nasa.jpl.aerie.constraints.model.SimulationResults convertToConstraintModelResults(
      SimulationResults driverResults)
  {
    final var planDuration = planningHorizon.getAerieHorizonDuration();

    return new gov.nasa.jpl.aerie.constraints.model.SimulationResults(
        Window.between(Duration.ZERO, planDuration),
        driverResults.simulatedActivities.entrySet().stream()
                                         .map(e -> convertToConstraintModelActivityInstance(e.getKey(), e.getValue()))
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
      String id, SimulatedActivity driverActivity)
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

  private <T> List<Pair<Duration, T>> deserialize(List<Pair<Duration, SerializedValue>> values, ValueMapper<T> mapper) {
    final var deserialized = new ArrayList<Pair<Duration, T>>();
    for (final var el : values) {
      var des = mapper.deserializeValue(el.getValue()).getSuccessOrThrow();
      deserialized.add(Pair.of(el.getKey(), des));
    }
    return deserialized;
  }

  private SerializedValue serialize(Object paramValue) {
    if (paramValue instanceof String) {
      return SerializedValue.of((String) paramValue);
    }
    if (paramValue instanceof Enum) {
      return SerializedValue.of(((Enum<?>) paramValue).name());
    }
    if (paramValue instanceof Long) {
      return SerializedValue.of((Long) paramValue);
    }
    if (paramValue instanceof Integer) {
      return SerializedValue.of((Integer) paramValue);
    }
    if (paramValue instanceof Double) {
      return SerializedValue.of((Double) paramValue);
    }
    if (paramValue instanceof Boolean) {
      return SerializedValue.of((Boolean) paramValue);
    }
    if (paramValue instanceof Duration) {
      return SerializedValue.of( ((Duration) paramValue).in(MICROSECONDS));
    }
    throw new RuntimeException("Parameter type not supported");
  }

}
