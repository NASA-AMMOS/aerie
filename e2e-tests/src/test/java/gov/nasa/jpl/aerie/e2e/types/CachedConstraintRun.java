package gov.nasa.jpl.aerie.e2e.types;

import javax.json.JsonObject;
import java.util.Optional;

public record CachedConstraintRun(
      int constraintId,
      int constraintRevision,
      int simDatasetId,
      String constraintDefinition,
      Optional<ConstraintResult> results
  ) {
  public static CachedConstraintRun fromJSON(JsonObject json) {
    return new CachedConstraintRun(
        json.getInt("constraint_id"),
        json.getInt("constraint_revision"),
        json.getInt("simulation_dataset_id"),
        json.getJsonObject("constraint_definition").getString("definition"),
        json.getJsonObject("results").isEmpty() ?
            Optional.empty() :
            Optional.of(ConstraintResult.fromJSON(json.getJsonObject("results")))
    );
  }
}
