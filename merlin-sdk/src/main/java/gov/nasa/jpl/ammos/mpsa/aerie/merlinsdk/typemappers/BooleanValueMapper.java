package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class BooleanValueMapper implements ValueMapper<Boolean> {
  @Override
  public ParameterSchema getValueSchema() {
    return ParameterSchema.BOOLEAN;
  }

  @Override
  public Result<Boolean, String> deserializeValue(final SerializedParameter serializedValue) {
    return serializedValue
        .asBoolean()
        .map(Result::<Boolean, String>success)
        .orElseGet(() -> Result.failure("Expected boolean, got " + serializedValue.toString()));
  }

  @Override
  public SerializedParameter serializeValue(final Boolean value) {
    return SerializedParameter.of(value);
  }
}
