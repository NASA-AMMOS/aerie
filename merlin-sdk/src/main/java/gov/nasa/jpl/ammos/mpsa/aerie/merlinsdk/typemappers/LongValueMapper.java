package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class LongValueMapper implements ValueMapper<Long> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.INT;
  }

  @Override
  public Result<Long, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asInt()
        .map(Result::<Long, String>success)
        .orElseGet(() -> Result.failure("Expected integral number, got " + serializedValue.toString()));
  }

  @Override
  public SerializedValue serializeValue(final Long value) {
    return SerializedValue.of(value);
  }
}
