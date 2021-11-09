package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.services.AdaptationService;
import gov.nasa.jpl.aerie.merlin.server.services.CreateSimulationMessage;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StubAdaptationService implements AdaptationService {
  public static final String EXISTENT_ADAPTATION_ID = "abc";
  public static final String NONEXISTENT_ADAPTATION_ID = "def";
  public static final AdaptationJar EXISTENT_ADAPTATION;

  public static final String EXISTENT_ACTIVITY_TYPE = "activity";
  public static final String NONEXISTENT_ACTIVITY_TYPE = "no-activity";
  public static final ActivityType EXISTENT_ACTIVITY = new ActivityType(
      EXISTENT_ACTIVITY_TYPE,
      List.of(new Parameter("Param", ValueSchema.STRING)));

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
      Instant.EPOCH);

  static {
    EXISTENT_ADAPTATION = new AdaptationJar();
    EXISTENT_ADAPTATION.name = "adaptation";
    EXISTENT_ADAPTATION.version = "1.0a";
    EXISTENT_ADAPTATION.mission = "mission";
    EXISTENT_ADAPTATION.owner = "Tester";
    EXISTENT_ADAPTATION.path = Path.of("existent-adaptation");
  }

  @Override
  public Map<String, AdaptationJar> getAdaptations() {
    return Map.of(EXISTENT_ADAPTATION_ID, EXISTENT_ADAPTATION);
  }

  @Override
  public AdaptationJar getAdaptationById(final String adaptationId) throws NoSuchAdaptationException {
    if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
      throw new NoSuchAdaptationException(adaptationId);
    }

    return EXISTENT_ADAPTATION;
  }

  @Override
  public Map<String, Constraint> getConstraints(final String adaptationId) throws NoSuchAdaptationException {
    return Map.of();
  }

  @Override
  public Map<String, ValueSchema> getStatesSchemas(final String adaptationId) throws NoSuchAdaptationException {
    if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
      throw new NoSuchAdaptationException(adaptationId);
    }

    return Map.of();
  }

  @Override
  public Map<String, ActivityType> getActivityTypes(final String adaptationId) throws NoSuchAdaptationException {
    if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
      throw new NoSuchAdaptationException(adaptationId);
    }

    return Map.of(EXISTENT_ACTIVITY_TYPE, EXISTENT_ACTIVITY);
  }

  @Override
  public List<String> validateActivityParameters(final String adaptationId, final SerializedActivity activityParameters)
  throws NoSuchAdaptationException
  {
    if (!Objects.equals(adaptationId, EXISTENT_ADAPTATION_ID)) {
      throw new NoSuchAdaptationException(adaptationId);
    }

    if (Objects.equals(activityParameters.getTypeName(), NONEXISTENT_ACTIVITY_INSTANCE.getTypeName())) {
      return NO_SUCH_ACTIVITY_TYPE_FAILURES;
    } else if (Objects.equals(activityParameters, UNCONSTRUCTABLE_ACTIVITY_INSTANCE)) {
      return UNCONSTRUCTABLE_ACTIVITY_INSTANCE_FAILURES;
    } else if (Objects.equals(activityParameters, INVALID_ACTIVITY_INSTANCE)) {
      return INVALID_ACTIVITY_INSTANCE_FAILURES;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public List<Parameter> getModelParameters(final String adaptationId) {
    return List.of();
  }

  @Override
  public SimulationResults runSimulation(final CreateSimulationMessage message) throws NoSuchAdaptationException {
    if (!Objects.equals(message.adaptationId(), EXISTENT_ADAPTATION_ID)) {
      throw new NoSuchAdaptationException(message.adaptationId());
    }

    return SUCCESSFUL_SIMULATION_RESULTS;
  }

  @Override
  public void refreshModelParameters(final String adaptationId) throws NoSuchAdaptationException
  {
  }

  @Override
  public void refreshActivityTypes(final String adaptationId) throws NoSuchAdaptationException
  {
  }
}
