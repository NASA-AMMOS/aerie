package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public record Discrete<V>(V extract) implements Dynamics<V, Discrete<V>> {
  @Override
  public Discrete<V> step(Duration t) {
    return this;
  }

  public static <V> Discrete<V> discrete(V value) {
    return new Discrete<>(value);
  }

  public static <V> ValueMapper<Discrete<V>> valueMapper(ValueMapper<V> mapper) {
    return new ValueMapper<Discrete<V>>() {
      @Override
      public ValueSchema getValueSchema() {
        return mapper.getValueSchema();
      }

      @Override
      public Result<Discrete<V>, String> deserializeValue(final SerializedValue serializedValue) {
        return Result.success(new Discrete<>(mapper.deserializeValue(serializedValue).getSuccessOrThrow()));
      }

      @Override
      public SerializedValue serializeValue(final Discrete<V> value) {
        return mapper.serializeValue(value.extract);
      }
    };
  }
}
