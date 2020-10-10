package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.function.Function;

public final class IntegerValueMapper implements ValueMapper<Integer> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.INT;
  }

  @Override
  public Result<Integer, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asInt()
        .map((Function<Long, Result<Long, String>>) Result::success)
        .orElseGet(() -> Result.failure("Expected integral number, got " + serializedValue.toString()))
        .andThen(x -> {
          final var y = x.intValue();
          if (x != y) {
            return Result.failure("Invalid parameter; value outside range of `int`");
          } else {
            return Result.success(y);
          }
        });
  }

  @Override
  public SerializedValue serializeValue(final Integer value) {
    return SerializedValue.of(value);
  }
}
