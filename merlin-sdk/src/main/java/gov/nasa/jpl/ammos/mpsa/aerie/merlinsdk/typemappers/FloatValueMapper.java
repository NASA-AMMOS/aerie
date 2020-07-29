package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class FloatValueMapper implements ValueMapper<Float> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.REAL;
  }

  @Override
  public Result<Float, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asReal()
        .map(Result::<Double, String>success)
        .orElseGet(() -> Result.failure("Expected real number, got " + serializedValue.toString()))
        .mapSuccess(Number::floatValue);
  }

  @Override
  public SerializedValue serializeValue(final Float value) {
    return SerializedValue.of(value);
  }
}

