package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class LongValueMapper implements ValueMapper<Long> {
  @Override
  public ParameterSchema getValueSchema() {
    return ParameterSchema.INT;
  }

  @Override
  public Result<Long, String> deserializeValue(final SerializedParameter serializedValue) {
    return serializedValue
        .asInt()
        .map(Result::<Long, String>success)
        .orElseGet(() -> Result.failure("Expected integral number, got " + serializedValue.toString()));
  }

  @Override
  public SerializedParameter serializeValue(final Long value) {
    return SerializedParameter.of(value);
  }
}
