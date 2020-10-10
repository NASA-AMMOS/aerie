package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class ByteValueMapper implements ValueMapper<Byte> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.INT;
  }

  @Override
  public Result<Byte, String> deserializeValue(final SerializedValue serializedValue) {
    return Result
        .from(serializedValue.asInt(), () -> "Expected integral number, got " + serializedValue.toString())
        .andThen(x -> {
          final var y = x.byteValue();
          if (x != y) {
            return Result.failure("Invalid parameter; value outside range of `byte`");
          } else {
            return Result.success(y);
          }
        });
  }

  @Override
  public SerializedValue serializeValue(final Byte value) {
    return SerializedValue.of(value);
  }
}
