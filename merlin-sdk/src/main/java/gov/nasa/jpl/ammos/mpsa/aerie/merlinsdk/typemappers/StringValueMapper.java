package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class StringValueMapper implements ValueMapper<String> {
  @Override
  public ParameterSchema getValueSchema() {
    return ParameterSchema.STRING;
  }

  @Override
  public Result<String, String> deserializeValue(final SerializedParameter serializedValue) {
    return serializedValue
        .asString()
        .map(Result::<String, String>success)
        .orElseGet(() -> Result.failure("Expected string, got " + serializedValue.toString()));
  }

  @Override
  public SerializedParameter serializeValue(final String value) {
    return SerializedParameter.of(value);
  }
}
