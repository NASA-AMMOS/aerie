package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class ByteParameterMapper implements ParameterMapper<Byte> {
  @Override
  public ParameterSchema getParameterSchema() {
    return ParameterSchema.INT;
  }

  @Override
  public Result<Byte, String> deserializeParameter(final SerializedParameter serializedParameter) {
    return serializedParameter
        .asInt()
        .map(Result::<Long, String>success)
        .orElseGet(() -> Result.failure("Expected integral number, got " + serializedParameter.toString()))
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
  public SerializedParameter serializeParameter(final Byte parameter) {
    return SerializedParameter.of(parameter);
  }
}
