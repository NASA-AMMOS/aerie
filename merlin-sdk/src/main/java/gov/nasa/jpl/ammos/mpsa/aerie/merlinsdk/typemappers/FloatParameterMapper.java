package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class FloatParameterMapper implements ParameterMapper<Float> {
  @Override
  public ParameterSchema getParameterSchema() {
    return ParameterSchema.DOUBLE;
  }

  @Override
  public Result<Float, String> deserializeParameter(final SerializedParameter serializedParameter) {
    return serializedParameter
        .asReal()
        .map(Result::<Double, String>success)
        .orElseGet(() -> Result.failure("Expected real number, got " + serializedParameter.toString()))
        .mapSuccess(Number::floatValue);
  }

  @Override
  public SerializedParameter serializeParameter(final Float parameter) {
    return SerializedParameter.of(parameter);
  }
}

