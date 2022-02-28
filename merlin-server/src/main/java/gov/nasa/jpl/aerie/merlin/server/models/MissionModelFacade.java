package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.model.ConfigurationType;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MissionModelFacade {
  private final MissionModel<?> missionModel;

  public MissionModelFacade(final MissionModel<?> missionModel) throws MissionModelContractException {
    this.missionModel = missionModel;
  }

  public SimulationResults simulate(
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule,
      final Duration simulationDuration,
      final Instant startTime
  ) {
    return SimulationDriver.simulate(this.missionModel, schedule, startTime, simulationDuration);
  }

  public Map<String, ValueSchema> getStateSchemas() {
    final var schemas = new HashMap<String, ValueSchema>();

    for (final var entry : this.missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      schemas.put(name, resource.getSchema());
    }

    return schemas;
  }

  public List<String> validateActivity(final String typeName, final Map<String, SerializedValue> arguments)
  throws NoSuchActivityTypeException, UnconstructableActivityInstanceException
  {
    final var specType = Optional
        .ofNullable(this.missionModel.getTaskSpecificationTypes().get(typeName))
        .orElseThrow(NoSuchActivityTypeException::new);

    return getValidationFailures(specType, arguments);
  }

  private <Specification, Return> List<String> getValidationFailures(
      final TaskSpecType<?, Specification, Return> specType,
      final Map<String, SerializedValue> arguments)
  throws UnconstructableActivityInstanceException
  {
    try {
      return specType.getValidationFailures(specType.instantiate(arguments));
    } catch (final TaskSpecType.UnconstructableTaskSpecException | MissingArgumentsException e) {
      throw new UnconstructableActivityInstanceException(
          "Unknown failure when deserializing activity -- do the parameters match the schema?",
          e);
    }
  }

  public Map<String, SerializedValue> getActivityEffectiveArguments(
      final String typeName,
      final Map<String, SerializedValue> arguments)
  throws NoSuchActivityTypeException, UnconstructableActivityInstanceException, MissingArgumentsException
  {
    final var specType = Optional
        .ofNullable(this.missionModel.getTaskSpecificationTypes().get(typeName))
        .orElseThrow(NoSuchActivityTypeException::new);

    return getActivityEffectiveArguments(specType, arguments);
  }

  private static <Specification, Return> Map<String, SerializedValue> getActivityEffectiveArguments(
      final TaskSpecType<?, Specification, Return> specType,
      final Map<String, SerializedValue> arguments)
  throws UnconstructableActivityInstanceException, MissingArgumentsException
  {
    try {
      final var activity = specType.instantiate(arguments);
      return specType.getArguments(activity);
    } catch (final TaskSpecType.UnconstructableTaskSpecException e) {
      throw new UnconstructableActivityInstanceException(
          "Unknown failure when deserializing activity -- do the parameters match the schema?",
          e);
    }
  }

  public List<String> validateConfiguration(final Map<String, SerializedValue> arguments)
  throws UnconfigurableMissionModelException, UnconstructableMissionModelConfigurationException
  {
    final var configType = this.missionModel.getConfigurationType()
        .orElseThrow(UnconfigurableMissionModelException::new);

    return getValidationFailures(configType, arguments);
  }

  private <Config> List<String> getValidationFailures(
      final ConfigurationType<Config> configurationType,
      final Map<String, SerializedValue> arguments)
  throws UnconstructableMissionModelConfigurationException
  {
    try {
      return configurationType.getValidationFailures(configurationType.instantiate(arguments));
    } catch (final ConfigurationType.UnconstructableConfigurationException | MissingArgumentsException e) {
      throw new UnconstructableMissionModelConfigurationException(
          "Unknown failure when deserializing configuration -- do the parameters match the schema?",
          e);
    }
  }

  /** Get mission model configuration effective arguments. */
  public Map<String, SerializedValue> getEffectiveArguments(final Map<String, SerializedValue> arguments)
  throws UnconfigurableMissionModelException, MissingArgumentsException, UnconstructableMissionModelConfigurationException
  {
    final var configType = this.missionModel.getConfigurationType()
        .orElseThrow(UnconfigurableMissionModelException::new);
    return getEffectiveArguments(configType, arguments);
  }

  private static <Config> Map<String, SerializedValue> getEffectiveArguments(
      final ConfigurationType<Config> configurationType,
      final Map<String, SerializedValue> arguments)
  throws MissingArgumentsException, UnconstructableMissionModelConfigurationException
  {
    try {
      final var config = configurationType.instantiate(arguments);
      return configurationType.getArguments(config);
    } catch (final ConfigurationType.UnconstructableConfigurationException e) {
      throw new UnconstructableMissionModelConfigurationException(
          "Unknown failure when deserializing configuration -- do the parameters match the schema?",
          e);
    }
  }

  public static final class Unconfigured<Model> {
    private final MissionModelFactory<Model> factory;

    public Unconfigured(final MissionModelFactory<Model> factory) {
      this.factory = factory;
    }

    public Map<String, ActivityType> getActivityTypes()
    throws MissionModelFacade.MissionModelContractException
    {
      final var activityTypes = new HashMap<String, ActivityType>();
      factory.getTaskSpecTypes().forEach((name, specType) -> {
        activityTypes.put(name, new ActivityType(name, specType.getParameters(), specType.getRequiredParameters(), specType.getReturnValueSchema()));
      });
      return activityTypes;
    }

    public ActivityType getActivityType(final String typeName)
    throws MissionModelFacade.NoSuchActivityTypeException, MissionModelFacade.MissionModelContractException
    {
      final var specType = Optional
          .ofNullable(factory.getTaskSpecTypes().get(typeName))
          .orElseThrow(MissionModelFacade.NoSuchActivityTypeException::new);

      return new ActivityType(typeName, specType.getParameters(), specType.getRequiredParameters(), specType.getReturnValueSchema());
    }

    public List<Parameter> getParameters() {
      return this.factory.getConfigurationType().map(ConfigurationType::getParameters).orElseGet(List::of);
    }
  }

  public static class MissionModelContractException extends RuntimeException {
    public MissionModelContractException(final String message) {
      super(message);
    }

    public MissionModelContractException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  public static class NoSuchActivityTypeException extends Exception {}

  public static class UnconstructableActivityInstanceException extends Exception {
    public UnconstructableActivityInstanceException(final String message) {
      super(message);
    }

    public UnconstructableActivityInstanceException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  public static class UnconfigurableMissionModelException extends Exception {}

  public static class UnconstructableMissionModelConfigurationException extends Exception {
    public UnconstructableMissionModelConfigurationException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
