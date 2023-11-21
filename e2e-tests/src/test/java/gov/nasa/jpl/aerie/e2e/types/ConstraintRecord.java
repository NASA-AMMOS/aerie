package gov.nasa.jpl.aerie.e2e.types;

import javax.json.JsonObject;
import java.util.List;
import java.util.Optional;

public record ConstraintRecord(
    boolean success,
    Optional<ConstraintResult> result,
    List<ConstraintError> errors

  ) {
  public static ConstraintRecord fromJSON(JsonObject json){
    return new ConstraintRecord(
        json.getBoolean("success"),
        json.getJsonObject("results").isEmpty() ? Optional.empty() : Optional.of(ConstraintResult.fromJSON(json.getJsonObject("results"))),
        json.getJsonArray("errors").getValuesAs(ConstraintError::fromJSON));
  }
}
