package gov.nasa.jpl.aerie.contrib.serialization.mappers;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.aerie.utilities.Result;

import java.util.function.Function;

public final class ByteValueMapper implements ValueMapper<Byte> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.INT;
  }

  @Override
  public Result<Byte, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asInt()
        .map((Function<Long, Result<Long, String>>) Result::success)
        .orElseGet(() -> Result.failure("Expected integral number, got " + serializedValue.toString()))
        .match(
            (Long x) -> {
              final var y = x.byteValue();
              if (x != y) {
                return Result.failure("Invalid parameter; value outside range of `byte`");
              } else {
                return Result.success(y);
              }
            },
            Result::failure
        );
  }

  @Override
  public SerializedValue serializeValue(final Byte value) {
    return SerializedValue.of(value);
  }
}
