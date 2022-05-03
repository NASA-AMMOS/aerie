package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.framework.VoidEnum;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelFacade;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.services.CreateSimulationMessage;
import gov.nasa.jpl.aerie.merlin.server.services.LocalMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class StubMissionModelService implements MissionModelService {
  public static final String EXISTENT_MISSION_MODEL_ID = "abc";
  public static final String NONEXISTENT_MISSION_MODEL_ID = "def";
  public static final MissionModelJar EXISTENT_MISSION_MODEL;

  public static final String EXISTENT_ACTIVITY_TYPE = "activity";
  public static final String NONEXISTENT_ACTIVITY_TYPE = "no-activity";
  public static final ActivityType EXISTENT_ACTIVITY = new ActivityType(
      EXISTENT_ACTIVITY_TYPE,
      List.of(new Parameter("Param", ValueSchema.STRING)),
      List.of(),
      new EnumValueMapper<>(VoidEnum.class).getValueSchema());

  public static final SerializedActivity VALID_ACTIVITY_INSTANCE = new SerializedActivity(
      EXISTENT_ACTIVITY_TYPE,
      Map.of("Param", SerializedValue.of("Value")));
  public static final SerializedActivity INVALID_ACTIVITY_INSTANCE = new SerializedActivity(
      EXISTENT_ACTIVITY_TYPE,
      Map.of("Param", SerializedValue.of("")));
  public static final SerializedActivity UNCONSTRUCTABLE_ACTIVITY_INSTANCE = new SerializedActivity(
      EXISTENT_ACTIVITY_TYPE,
      Map.of("Nonexistent", SerializedValue.of("Value")));
  public static final SerializedActivity NONEXISTENT_ACTIVITY_INSTANCE = new SerializedActivity(
      NONEXISTENT_ACTIVITY_TYPE,
      Map.of());

  public static final List<String> NO_SUCH_ACTIVITY_TYPE_FAILURES = List.of("no such activity type");
  public static final List<String> INVALID_ACTIVITY_INSTANCE_FAILURES = List.of("just wrong");
  public static final List<String> UNCONSTRUCTABLE_ACTIVITY_INSTANCE_FAILURES = List.of(
      "Unconstructable activity instance");

  public static final SimulationResults SUCCESSFUL_SIMULATION_RESULTS = new SimulationResults(
      Map.of(),
      Map.of(),
      Map.of(),
      Map.of(),
      Instant.EPOCH,
      List.of(),
      new TreeMap<>());

  static {
    EXISTENT_MISSION_MODEL = new MissionModelJar();
    EXISTENT_MISSION_MODEL.name = "missionModel";
    EXISTENT_MISSION_MODEL.version = "1.0a";
    EXISTENT_MISSION_MODEL.mission = "mission";
    EXISTENT_MISSION_MODEL.owner = "Tester";
    EXISTENT_MISSION_MODEL.path = Path.of("existent-missionModel");
  }

  @Override
  public Map<String, MissionModelJar> getMissionModels() {
    return Map.of(EXISTENT_MISSION_MODEL_ID, EXISTENT_MISSION_MODEL);
  }

  @Override
  public MissionModelJar getMissionModelById(final String missionModelId) throws NoSuchMissionModelException {
    if (!Objects.equals(missionModelId, EXISTENT_MISSION_MODEL_ID)) {
      throw new NoSuchMissionModelException(missionModelId);
    }

    return EXISTENT_MISSION_MODEL;
  }

  @Override
  public Map<String, Constraint> getConstraints(final String missionModelId) throws NoSuchMissionModelException {
    return Map.of();
  }

  @Override
  public Map<String, ValueSchema> getStatesSchemas(final String missionModelId) throws NoSuchMissionModelException {
    if (!Objects.equals(missionModelId, EXISTENT_MISSION_MODEL_ID)) {
      throw new NoSuchMissionModelException(missionModelId);
    }

    return Map.of();
  }

  @Override
  public Map<String, ActivityType> getActivityTypes(final String missionModelId) throws NoSuchMissionModelException {
    if (!Objects.equals(missionModelId, EXISTENT_MISSION_MODEL_ID)) {
      throw new NoSuchMissionModelException(missionModelId);
    }

    return Map.of(EXISTENT_ACTIVITY_TYPE, EXISTENT_ACTIVITY);
  }

  @Override
  public List<String> validateActivityArguments(final String missionModelId, final SerializedActivity activity)
  throws NoSuchMissionModelException
  {
    if (!Objects.equals(missionModelId, EXISTENT_MISSION_MODEL_ID)) {
      throw new NoSuchMissionModelException(missionModelId);
    }

    if (Objects.equals(activity.getTypeName(), NONEXISTENT_ACTIVITY_INSTANCE.getTypeName())) {
      return NO_SUCH_ACTIVITY_TYPE_FAILURES;
    } else if (Objects.equals(activity, UNCONSTRUCTABLE_ACTIVITY_INSTANCE)) {
      return UNCONSTRUCTABLE_ACTIVITY_INSTANCE_FAILURES;
    } else if (Objects.equals(activity, INVALID_ACTIVITY_INSTANCE)) {
      return INVALID_ACTIVITY_INSTANCE_FAILURES;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public <T> Map<T, String> validateActivityInstantiations(
      final String missionModelId,
      final Map<T, SerializedActivity> activities)
  throws NoSuchMissionModelException, MissionModelFacade.MissionModelContractException, LocalMissionModelService.MissionModelLoadException
  {
    return Map.of();
  }

  @Override
  public Map<String, SerializedValue> getActivityEffectiveArguments(
      final String missionModelId,
      final SerializedActivity activity)
  {
    return Map.of();
  }

  @Override
  public List<String> validateModelArguments(final String missionModelId, final Map<String, SerializedValue> arguments)
  throws LocalMissionModelService.MissionModelLoadException
  {
    return List.of();
  }

  @Override
  public List<Parameter> getModelParameters(final String missionModelId) {
    return List.of();
  }

  @Override
  public Map<String, SerializedValue> getModelEffectiveArguments(
      final String missionModelId,
      final Map<String, SerializedValue> arguments)
  throws MissingArgumentsException, LocalMissionModelService.MissionModelLoadException
  {
    return Map.of();
  }

  @Override
  public SimulationResults runSimulation(final CreateSimulationMessage message) throws NoSuchMissionModelException {
    if (!Objects.equals(message.missionModelId(), EXISTENT_MISSION_MODEL_ID)) {
      throw new NoSuchMissionModelException(message.missionModelId());
    }

    return SUCCESSFUL_SIMULATION_RESULTS;
  }

  @Override
  public void refreshModelParameters(final String missionModelId) throws NoSuchMissionModelException
  {
  }

  @Override
  public void refreshActivityTypes(final String missionModelId) throws NoSuchMissionModelException
  {
  }
}
