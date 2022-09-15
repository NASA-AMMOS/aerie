package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.model.OutputType;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Map;
import java.util.Objects;

public final class Registrar {
  private final Initializer builder;

  public Registrar(final Initializer builder) {
    this.builder = Objects.requireNonNull(builder);
  }

  public boolean isInitializationComplete() {
    return (ModelActions.context.get().getContextType() != Context.ContextType.Initializing);
  }

  public <Value> void discrete(final String name, final Resource<Value> resource, final ValueMapper<Value> mapper) {
    addDiscreteResource(this.builder, name, resource, mapper);
  }

  public void real(final String name, final Resource<RealDynamics> resource) {
    addRealResource(this.builder, name, resource);
  }

  private static <Value> void addDiscreteResource(
      final Initializer initializer,
      final String name,
      final Resource<Value> resource,
      final ValueMapper<Value> mapper
  ) {
    initializer.resource(name, new gov.nasa.jpl.aerie.merlin.protocol.model.Resource<Value>() {
      @Override
      public OutputType<Value> getOutputType() {
        return new OutputType<>() {
          @Override
          public ValueSchema getSchema() {
            return mapper.getValueSchema();
          }

          @Override
          public SerializedValue serialize(final Value value) {
            return mapper.serializeValue(value);
          }
        };
      }

      @Override
      public String getType() {
        return "discrete";
      }

      @Override
      public Value getDynamics(final Querier querier) {
        try (final var _token = ModelActions.context.set(new QueryContext(querier))) {
          return resource.getDynamics();
        }
      }
    });
  }

  private static void addRealResource(
      final Initializer initializer,
      final String name,
      final Resource<RealDynamics> resource
  ) {
    initializer.resource(name, new gov.nasa.jpl.aerie.merlin.protocol.model.Resource<RealDynamics>() {
      @Override
      public OutputType<RealDynamics> getOutputType() {
        return new OutputType<>() {
          @Override
          public ValueSchema getSchema() {
            return ValueSchema.ofStruct(Map.of(
                "initial", ValueSchema.REAL,
                "rate", ValueSchema.REAL));
          }

          @Override
          public SerializedValue serialize(final RealDynamics dynamics) {
            return SerializedValue.of(Map.of(
                "initial", SerializedValue.of(dynamics.initial),
                "rate", SerializedValue.of(dynamics.rate)));
          }
        };
      }

      @Override
      public String getType() {
        return "real";
      }

      @Override
      public RealDynamics getDynamics(final Querier querier) {
        try (final var _token = ModelActions.context.set(new QueryContext(querier))) {
          return resource.getDynamics();
        }
      }
    });
  }

  public <Event> void topic(final String name, final CellRef<Event,?> ref, final ValueMapper<Event> mapper) {
    Objects.requireNonNull(mapper);

    this.builder.topic(name, ref.topic, new OutputType<>() {
      @Override
      public ValueSchema getSchema() {
        return mapper.getValueSchema();
      }

      @Override
      public SerializedValue serialize(final Event value) {
        return mapper.serializeValue(value);
      }
    });
  }
}
