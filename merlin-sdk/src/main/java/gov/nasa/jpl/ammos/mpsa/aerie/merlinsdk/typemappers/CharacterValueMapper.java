package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class CharacterValueMapper implements ValueMapper<Character> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.STRING;
  }

  @Override
  public Result<Character, String> deserializeValue(final SerializedValue serializedValue) {
    return Result
        .from(serializedValue.asString(), () -> "Expected string, got " + serializedValue.toString())
        .andThen(string -> {
          if (string.length() != 1) {
            return Result.failure("Expected single-character string");
          } else {
            return Result.success(string.charAt(0));
          }
        });
  }

  @Override
  public SerializedValue serializeValue(final Character value) {
    return SerializedValue.of(Character.toString(value));
  }
}
