package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.model.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AdaptationFacade<$Schema> {
  private final Adaptation<$Schema, ?> adaptation;

  public AdaptationFacade(final Adaptation<$Schema, ?> adaptation) throws AdaptationContractException {
    this.adaptation = adaptation;
  }

  public SimulationResults simulate(
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Duration simulationDuration,
      final Instant startTime
  ) {
    return SimulationDriver.simulate(this.adaptation, schedule, startTime, simulationDuration);
  }

  public Map<String, ValueSchema> getStateSchemas() {
    final var schemas = new HashMap<String, ValueSchema>();

    for (final var entry : this.adaptation.getResources().entrySet()) {
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
        .ofNullable(this.adaptation.getTaskSpecificationTypes().get(typeName))
        .orElseThrow(NoSuchActivityTypeException::new);

    return getValidationFailures(specType, arguments);
  }

  private <Specification> List<String> getValidationFailures(
      final TaskSpecType<?, Specification> specType,
      final Map<String, SerializedValue> arguments)
  throws UnconstructableActivityInstanceException
  {
    try {
      return specType.getValidationFailures(specType.instantiate(arguments));
    } catch (final TaskSpecType.UnconstructableTaskSpecException e) {
      throw new UnconstructableActivityInstanceException(
          "Unknown failure when deserializing activity -- do the parameters match the schema?",
          e);
    }
  }

  public Map<String, SerializedValue> getActivityEffectiveArguments(
      final String typeName,
      final Map<String, SerializedValue> arguments)
  throws NoSuchActivityTypeException, UnconstructableActivityInstanceException, MissingArgumentException
  {
    final var specType = Optional
        .ofNullable(this.adaptation.getTaskSpecificationTypes().get(typeName))
        .orElseThrow(NoSuchActivityTypeException::new);

    return getActivityEffectiveArguments(specType, arguments);
  }

  private static <Specification> Map<String, SerializedValue> getActivityEffectiveArguments(
      final TaskSpecType<?, Specification> specType,
      final Map<String, SerializedValue> arguments)
  throws UnconstructableActivityInstanceException, MissingArgumentException
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

  public static final class Unconfigured<Model> {
    private final AdaptationFactory<Model> factory;

    public Unconfigured(final AdaptationFactory<Model> factory) {
      this.factory = factory;
    }

    public Map<String, ActivityType> getActivityTypes()
    throws AdaptationFacade.AdaptationContractException
    {
      final var activityTypes = new HashMap<String, ActivityType>();
      factory.getTaskSpecTypes().forEach((name, specType) -> {
        activityTypes.put(name, new ActivityType(name, specType.getParameters()));
      });
      return activityTypes;
    }

    public ActivityType getActivityType(final String typeName)
    throws AdaptationFacade.NoSuchActivityTypeException, AdaptationFacade.AdaptationContractException
    {
      final var specType = Optional
          .ofNullable(factory.getTaskSpecTypes().get(typeName))
          .orElseThrow(AdaptationFacade.NoSuchActivityTypeException::new);

      return new ActivityType(typeName, specType.getParameters());
    }

    public List<Parameter> getParameters() {
      return factory.getParameters();
    }
  }

  public static class AdaptationContractException extends RuntimeException {
    public AdaptationContractException(final String message) {
      super(message);
    }

    public AdaptationContractException(final String message, final Throwable cause) {
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
}
