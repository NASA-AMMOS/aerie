package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class StringValueMapper implements ValueMapper<String> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.STRING;
  }

  @Override
  public Result<String, String> deserializeValue(final SerializedValue serializedValue) {
    return Result.from(serializedValue.asString(), () -> "Expected string, got " + serializedValue.toString());
  }

  @Override
  public SerializedValue serializeValue(final String value) {
    return SerializedValue.of(value);
  }
}
