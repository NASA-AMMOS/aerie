package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InvalidArgumentsException;
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

  public MissionModelFacade(final MissionModel<?> missionModel) {
    this.missionModel = missionModel;
  }

  public SimulationResults simulate(
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule,
      final Duration simulationDuration,
      final Instant startTime
  ) {
    return SimulationDriver.simulate(this.missionModel, schedule, startTime, simulationDuration);
  }

  public Map<String, ValueSchema> getResourceSchemas() {
    final var schemas = new HashMap<String, ValueSchema>();

    for (final var entry : this.missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      schemas.put(name, resource.getSchema());
    }

    return schemas;
  }

  public static final class Unconfigured<Model> {
    private final MissionModelFactory<?, ?, Model> factory;
    private final DirectiveTypeRegistry<?, Model> registry;

    public Unconfigured(final MissionModelFactory<?, ?, Model> factory) {
      this.factory = factory;
      this.registry = DirectiveTypeRegistry.extract(this.factory);
    }

    public Map<String, ActivityType> getActivityTypes() {
      final var activityTypes = new HashMap<String, ActivityType>();
      this.registry.taskSpecTypes().forEach((name, specType) -> {
        activityTypes.put(name, new ActivityType(name, specType.getParameters(), specType.getRequiredParameters(), specType.getReturnValueSchema()));
      });
      return activityTypes;
    }

    public ActivityType getActivityType(final String typeName)
    throws MissionModelFacade.NoSuchActivityTypeException
    {
      final var specType = Optional
          .ofNullable(this.registry.taskSpecTypes().get(typeName))
          .orElseThrow(() -> new NoSuchActivityTypeException(typeName));

      return new ActivityType(typeName, specType.getParameters(), specType.getRequiredParameters(), specType.getReturnValueSchema());
    }

    public Map<String, SerializedValue> getActivityEffectiveArguments(final SerializedActivity activity)
    throws NoSuchActivityTypeException, InvalidArgumentsException
    {
      final var specType = Optional
          .ofNullable(registry.taskSpecTypes().get(activity.getTypeName()))
          .orElseThrow(() -> new NoSuchActivityTypeException(activity.getTypeName()));
      return specType.getEffectiveArguments(activity.getArguments());
    }

    public List<String> validateActivityArguments(final SerializedActivity activity)
    throws NoSuchActivityTypeException, InvalidArgumentsException
    {
      final var specType = Optional
          .ofNullable(this.registry.taskSpecTypes().get(activity.getTypeName()))
          .orElseThrow(() -> new NoSuchActivityTypeException(activity.getTypeName()));
      return specType.validateArguments(activity.getArguments());
    }

    public List<Parameter> getParameters() {
      return this.factory.getConfigurationType().getParameters();
    }

    public Map<String, SerializedValue> getMissionModelEffectiveArguments(final Map<String, SerializedValue> arguments)
    throws InvalidArgumentsException
    {
      return this.factory
          .getConfigurationType()
          .getEffectiveArguments(arguments);
    }

    public List<String> validateMissionModelArguments(final Map<String, SerializedValue> arguments)
    throws InvalidArgumentsException
    {
      return this.factory
          .getConfigurationType()
          .validateArguments(arguments);
    }
  }

  public static class NoSuchActivityTypeException extends Exception {
    public final String typeName;

    public NoSuchActivityTypeException(final String typeName) {
      super("No such activity type: \"%s\"".formatted(typeName));
      this.typeName = typeName;
    }
  }
}
