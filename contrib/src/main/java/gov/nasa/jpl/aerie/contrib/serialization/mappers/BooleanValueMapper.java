package gov.nasa.jpl.aerie.contrib.serialization.mappers;

import gov.nasa.jpl.aerie.merlin.protocol.Result;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;

import java.util.function.Function;

public final class BooleanValueMapper implements ValueMapper<Boolean> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.BOOLEAN;
  }

  @Override
  public Result<Boolean, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asBoolean()
        .map((Function<Boolean, Result<Boolean, String>>) Result::success)
        .orElseGet(() -> Result.failure("Expected boolean, got " + serializedValue.toString()));
  }

  @Override
  public SerializedValue serializeValue(final Boolean value) {
    return SerializedValue.of(value);
  }
}
