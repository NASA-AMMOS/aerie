package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class LongValueMapper implements ValueMapper<Long> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.INT;
  }

  @Override
  public Result<Long, String> deserializeValue(final SerializedValue serializedValue) {
    return Result.from(serializedValue.asInt(), () -> "Expected integral number, got " + serializedValue.toString());
  }

  @Override
  public SerializedValue serializeValue(final Long value) {
    return SerializedValue.of(value);
  }
}
