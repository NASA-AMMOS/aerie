package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class IntegerValueMapper implements ValueMapper<Integer> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.INT;
  }

  @Override
  public Result<Integer, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asInt()
        .map(Result::<Long, String>success)
        .orElseGet(() -> Result.failure("Expected integral number, got " + serializedValue.toString()))
        .match(
            (Long x) -> {
              final var y = x.intValue();
              if (x != y) {
                return Result.failure("Invalid parameter; value outside range of `int`");
              } else {
                return Result.success(y);
              }
            },
            Result::failure
        );
  }

  @Override
  public SerializedValue serializeValue(final Integer value) {
    return SerializedValue.of(value);
  }
}
