package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class ShortValueMapper implements ValueMapper<Short> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.INT;
  }

  @Override
  public Result<Short, String> deserializeValue(final SerializedValue serializedValue) {
    return Result
        .from(serializedValue.asInt(), () -> "Expected integral number, got " + serializedValue.toString())
        .andThen(x -> {
          final var y = x.shortValue();
          if (x != y) {
            return Result.failure("Invalid parameter; value outside range of `short`");
          } else {
            return Result.success(y);
          }
        });
  }

  @Override
  public SerializedValue serializeValue(final Short value) {
    return SerializedValue.of(value);
  }
}
