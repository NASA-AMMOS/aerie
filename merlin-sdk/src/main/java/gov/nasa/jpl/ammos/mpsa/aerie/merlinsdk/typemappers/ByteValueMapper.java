package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class ByteValueMapper implements ValueMapper<Byte> {
  @Override
  public ParameterSchema getValueSchema() {
    return ParameterSchema.INT;
  }

  @Override
  public Result<Byte, String> deserializeValue(final SerializedParameter serializedValue) {
    return serializedValue
        .asInt()
        .map(Result::<Long, String>success)
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
  public SerializedParameter serializeValue(final Byte value) {
    return SerializedParameter.of(value);
  }
}
