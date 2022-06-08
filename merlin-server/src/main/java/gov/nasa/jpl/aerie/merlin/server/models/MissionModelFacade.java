package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
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

  public List<String> validateActivity(final SerializedActivity activity)
  throws NoSuchActivityTypeException, TaskSpecType.UnconstructableTaskSpecException, MissingArgumentsException
  {
    final var specType = Optional
        .ofNullable(this.missionModel.getDirectiveTypes().taskSpecTypes().get(activity.getTypeName()))
        .orElseThrow(() -> new NoSuchActivityTypeException(activity.getTypeName()));

    return getValidationFailures(specType, activity.getArguments());
  }

  private <Specification, Return> List<String> getValidationFailures(
      final TaskSpecType<?, Specification, Return> specType,
      final Map<String, SerializedValue> arguments)
  throws TaskSpecType.UnconstructableTaskSpecException, MissingArgumentsException
  {
    return specType.getValidationFailures(specType.instantiate(arguments));
  }

  public Map<String, SerializedValue> getActivityEffectiveArguments(
      final String typeName,
      final Map<String, SerializedValue> arguments)
  throws NoSuchActivityTypeException, TaskSpecType.UnconstructableTaskSpecException, MissingArgumentsException
  {
    final var specType = Optional
        .ofNullable(this.missionModel.getDirectiveTypes().taskSpecTypes().get(typeName))
        .orElseThrow(() -> new NoSuchActivityTypeException(typeName));

    return getActivityEffectiveArguments(specType, arguments);
  }

  private static <Specification, Return> Map<String, SerializedValue> getActivityEffectiveArguments(
      final TaskSpecType<?, Specification, Return> specType,
      final Map<String, SerializedValue> arguments)
  throws TaskSpecType.UnconstructableTaskSpecException, MissingArgumentsException
  {
    final var activity = specType.instantiate(arguments);
    return specType.getArguments(activity);
  }

  public List<String> validateConfiguration(final Map<String, SerializedValue> arguments)
  throws UnconfigurableMissionModelException, UnconstructableMissionModelConfigurationException
  {
    return getValidationFailures(this.missionModel.getConfigurationType(), arguments);
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
    return getEffectiveArguments(this.missionModel.getConfigurationType(), arguments);
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

  /** Get activity instantiation failure messages as a mapping of activity instance ID to failure. */
  public Map<ActivityInstanceId, String> validateActivityInstantiations(final Map<ActivityInstanceId, SerializedActivity> activities)
  {
    final var failures = new HashMap<ActivityInstanceId, String>();

    activities.forEach((id, act) -> {
      try {
        getActivityEffectiveArguments(act.getTypeName(), act.getArguments());
      } catch (final NoSuchActivityTypeException |
          TaskSpecType.UnconstructableTaskSpecException |
          MissingArgumentsException e)
      {
        failures.put(id, e.toString());
      }
    });

    return failures;
  }

  public static final class Unconfigured<Model> {
    private final MissionModelFactory<?, ?, Model> factory;
    private final DirectiveTypeRegistry<?, Model> registry;

    public Unconfigured(final MissionModelFactory<?, ?, Model> factory) {
      this.factory = factory;
      this.registry = DirectiveTypeRegistry.extract(this.factory);
    }

    public Map<String, ActivityType> getActivityTypes()
    throws MissionModelFacade.MissionModelContractException
    {
      final var activityTypes = new HashMap<String, ActivityType>();
      this.registry.taskSpecTypes().forEach((name, specType) -> {
        activityTypes.put(name, new ActivityType(name, specType.getParameters(), specType.getRequiredParameters(), specType.getReturnValueSchema()));
      });
      return activityTypes;
    }

    public ActivityType getActivityType(final String typeName)
    throws MissionModelFacade.NoSuchActivityTypeException, MissionModelFacade.MissionModelContractException
    {
      final var specType = Optional
          .ofNullable(this.registry.taskSpecTypes().get(typeName))
          .orElseThrow(() -> new NoSuchActivityTypeException(typeName));

      return new ActivityType(typeName, specType.getParameters(), specType.getRequiredParameters(), specType.getReturnValueSchema());
    }

    public List<Parameter> getParameters() {
      return this.factory.getConfigurationType().getParameters();
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

  public static class NoSuchActivityTypeException extends Exception {
    public final String typeName;

    public NoSuchActivityTypeException(final String typeName) {
      super("No such activity type: \"%s\"".formatted(typeName));
      this.typeName = typeName;
    }
  }

  public static class UnconfigurableMissionModelException extends Exception {}

  public static class UnconstructableMissionModelConfigurationException extends Exception {
    public UnconstructableMissionModelConfigurationException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
