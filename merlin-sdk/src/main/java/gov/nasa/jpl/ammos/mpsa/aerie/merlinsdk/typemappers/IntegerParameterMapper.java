package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class IntegerParameterMapper implements ParameterMapper<Integer> {
  @Override
  public ParameterSchema getParameterSchema() {
    return ParameterSchema.INT;
  }

  @Override
  public Result<Integer, String> deserializeParameter(final SerializedParameter serializedParameter) {
    return serializedParameter
        .asInt()
        .map(Result::<Long, String>success)
        .orElseGet(() -> Result.failure("Expected integral number, got " + serializedParameter.toString()))
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
  public SerializedParameter serializeParameter(final Integer parameter) {
    return SerializedParameter.of(parameter);
  }
}
