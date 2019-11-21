package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class BooleanParameterMapper implements ParameterMapper<Boolean> {
  @Override
  public ParameterSchema getParameterSchema() {
    return ParameterSchema.BOOLEAN;
  }

  @Override
  public Result<Boolean, String> deserializeParameter(final SerializedParameter serializedParameter) {
    return serializedParameter
        .asBoolean()
        .map(Result::<Boolean, String>success)
        .orElseGet(() -> Result.failure("Expected boolean, got " + serializedParameter.toString()));
  }

  @Override
  public SerializedParameter serializeParameter(final Boolean parameter) {
    return SerializedParameter.of(parameter);
  }
}
