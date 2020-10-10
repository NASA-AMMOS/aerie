package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class BooleanValueMapper implements ValueMapper<Boolean> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.BOOLEAN;
  }

  @Override
  public Result<Boolean, String> deserializeValue(final SerializedValue serializedValue) {
    return Result.from(serializedValue.asBoolean(), () -> "Expected boolean, got " + serializedValue.toString());
  }

  @Override
  public SerializedValue serializeValue(final Boolean value) {
    return SerializedValue.of(value);
  }
}
