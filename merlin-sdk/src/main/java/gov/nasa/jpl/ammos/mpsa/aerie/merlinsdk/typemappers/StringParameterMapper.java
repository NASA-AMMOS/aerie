package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class StringParameterMapper implements ParameterMapper<String> {
  @Override
  public ParameterSchema getParameterSchema() {
    return ParameterSchema.STRING;
  }

  @Override
  public Result<String, String> deserializeParameter(final SerializedParameter serializedParameter) {
    return serializedParameter
        .asString()
        .map(Result::<String, String>success)
        .orElseGet(() -> Result.failure("Expected string, got " + serializedParameter.toString()));
  }

  @Override
  public SerializedParameter serializeParameter(final String parameter) {
    return SerializedParameter.of(parameter);
  }
}
