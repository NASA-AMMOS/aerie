package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.model.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.model.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.RealApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
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
  ) throws SimulationDriver.TaskSpecInstantiationException
  {
    return SimulationDriver.simulate(this.adaptation, schedule, startTime, simulationDuration);
  }

  public Map<String, ValueSchema> getStateSchemas() {
    final class SchemaGetter<T> implements ResourceSolver.ApproximatorVisitor<T, ValueSchema> {
      @Override
      public ValueSchema real(final RealApproximator<T> approximator) {
        return ValueSchema.REAL;
      }

      @Override
      public ValueSchema discrete(final DiscreteApproximator<T> approximator) {
        return approximator.getSchema();
      }
    }

    final var schemas = new HashMap<String, ValueSchema>();

    for (final var family : this.adaptation.getResourceFamilies()) {
      final var schema = family.getSolver().approximate(new SchemaGetter<>());

      for (final var name : family.getResources().keySet()) {
        schemas.put(name, schema);
      }
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

  private static <Specification> Map<String, SerializedValue> getDefaultArguments(final TaskSpecType<?, Specification> specType) {
    return specType.getArguments(specType.instantiateDefault());
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
        activityTypes.put(name, new ActivityType(name, specType.getParameters(), getDefaultArguments(specType)));
      });
      return activityTypes;
    }

    public ActivityType getActivityType(final String typeName)
    throws AdaptationFacade.NoSuchActivityTypeException, AdaptationFacade.AdaptationContractException
    {
      final var specType = Optional
          .ofNullable(factory.getTaskSpecTypes().get(typeName))
          .orElseThrow(AdaptationFacade.NoSuchActivityTypeException::new);

      return new ActivityType(typeName, specType.getParameters(), getDefaultArguments(specType));
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
      super(
          message,
          cause);
    }
  }
}
