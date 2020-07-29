package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class FloatValueMapper implements ValueMapper<Float> {
  @Override
  public ParameterSchema getValueSchema() {
    return ParameterSchema.REAL;
  }

  @Override
  public Result<Float, String> deserializeValue(final SerializedParameter serializedValue) {
    return serializedValue
        .asReal()
        .map(Result::<Double, String>success)
        .orElseGet(() -> Result.failure("Expected real number, got " + serializedValue.toString()))
        .mapSuccess(Number::floatValue);
  }

  @Override
  public SerializedParameter serializeValue(final Float value) {
    return SerializedParameter.of(value);
  }
}

