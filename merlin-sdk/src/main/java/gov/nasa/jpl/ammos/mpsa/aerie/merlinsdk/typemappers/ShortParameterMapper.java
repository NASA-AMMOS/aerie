package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class ShortParameterMapper implements ParameterMapper<Short> {
  @Override
  public ParameterSchema getParameterSchema() {
    return ParameterSchema.INT;
  }

  @Override
  public Result<Short, String> deserializeParameter(final SerializedParameter serializedParameter) {
    return serializedParameter
        .asInt()
        .map(Result::<Long, String>success)
        .orElseGet(() -> Result.failure("Expected integral number, got " + serializedParameter.toString()))
        .mapSuccess(Number::shortValue);
  }

  @Override
  public SerializedParameter serializeParameter(final Short parameter) {
    return SerializedParameter.of(parameter);
  }
}
