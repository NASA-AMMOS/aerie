package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class CharacterParameterMapper implements ParameterMapper<Character> {
  @Override
  public ParameterSchema getParameterSchema() {
    return ParameterSchema.STRING;
  }

  @Override
  public Result<Character, String> deserializeParameter(final SerializedParameter serializedParameter) {
    return serializedParameter
        .asString()
        .map(Result::<String, String>success)
        .orElseGet(() -> Result.failure("Expected string, got " + serializedParameter.toString()))
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
  public SerializedParameter serializeParameter(final Character parameter) {
    return SerializedParameter.of(parameter.toString());
  }
}
