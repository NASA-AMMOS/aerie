package gov.nasa.jpl.aerie.e2e.types;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.List;

public record ConstraintRecord(
    boolean success,
    ConstraintResult result,
    List<ConstraintError> errors

  ) {
  public static ConstraintRecord fromJSON(JsonObject json){
    return new ConstraintRecord(
        json.getBoolean("success"),
        json.getJsonObject("results").isEmpty() ? null : ConstraintResult.fromJSON(json.getJsonObject("results")),
        json.getJsonArray("errors").getValuesAs(ConstraintError::fromJSON));
  }
}
