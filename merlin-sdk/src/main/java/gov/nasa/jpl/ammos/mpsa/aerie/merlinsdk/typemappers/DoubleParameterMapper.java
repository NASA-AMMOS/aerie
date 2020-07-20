package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class DoubleParameterMapper implements ParameterMapper<Double> {
  @Override
  public ParameterSchema getParameterSchema() {
    return ParameterSchema.REAL;
  }

  @Override
  public Result<Double, String> deserializeParameter(final SerializedParameter serializedParameter) {
    return serializedParameter
        .asReal()
        .map(Result::<Double, String>success)
        .orElseGet(() -> Result.failure("Expected real number, got " + serializedParameter.toString()));
  }

  @Override
  public SerializedParameter serializeParameter(final Double parameter) {
    return SerializedParameter.of(parameter);
  }
}
