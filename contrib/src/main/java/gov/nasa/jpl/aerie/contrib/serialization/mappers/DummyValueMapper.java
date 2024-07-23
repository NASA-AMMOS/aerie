package gov.nasa.jpl.aerie.contrib.serialization.mappers;

import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Map;

public record DummyValueMapper<T>(T dummyValue) implements ValueMapper<T> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.ofStruct(Map.of());
  }

  @Override
  public Result<T, String> deserializeValue(final SerializedValue serializedValue) {
    return Result.success(dummyValue);
  }

  @Override
  public SerializedValue serializeValue(final T value) {
    return SerializedValue.of(Map.of());
  }
}
