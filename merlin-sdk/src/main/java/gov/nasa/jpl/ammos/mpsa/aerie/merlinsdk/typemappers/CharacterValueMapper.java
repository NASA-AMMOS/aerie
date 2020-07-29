package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class CharacterValueMapper implements ValueMapper<Character> {
  @Override
  public ParameterSchema getValueSchema() {
    return ParameterSchema.STRING;
  }

  @Override
  public Result<Character, String> deserializeValue(final SerializedParameter serializedValue) {
    return serializedValue
        .asString()
        .map(Result::<String, String>success)
        .orElseGet(() -> Result.failure("Expected string, got " + serializedValue.toString()))
        .match(
            string -> {
              if (string.length() != 1) {
                return Result.failure("Expected single-character string");
              } else {
                return Result.success(string.charAt(0));
              }
            },
            Result::failure
        );
  }

  @Override
  public SerializedParameter serializeValue(final Character value) {
    return SerializedParameter.of(Character.toString(value));
  }
}
