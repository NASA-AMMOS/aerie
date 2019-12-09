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
        .mapSuccess(Number::intValue);
  }

  @Override
  public SerializedParameter serializeParameter(final Integer parameter) {
    return SerializedParameter.of(parameter);
  }
}
