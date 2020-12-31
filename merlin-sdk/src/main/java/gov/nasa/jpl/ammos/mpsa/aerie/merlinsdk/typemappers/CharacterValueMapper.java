package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.function.Function;

public final class CharacterValueMapper implements ValueMapper<Character> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.STRING;
  }

  @Override
  public Result<Character, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asString()
        .map((Function<String, Result<String, String>>) Result::success)
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
