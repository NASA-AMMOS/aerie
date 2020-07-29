package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class CharacterValueMapper implements ValueMapper<Character> {
  @Override
  public ParameterSchema getValueSchema() {
    return ParameterSchema.STRING;
  }

  @Override
  public Result<Character, String> deserializeValue(final SerializedValue serializedValue) {
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
  public SerializedValue serializeValue(final Character value) {
    return SerializedValue.of(Character.toString(value));
  }
}
