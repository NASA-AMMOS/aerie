package gov.nasa.jpl.aerie.e2e.types;

import javax.json.JsonObject;
import java.util.Optional;

public record CachedConstraintRun(
      int constraintId,
      int simDatasetId,
      String constraintDefinition,
      boolean definitionOutdated,
      Optional<ConstraintRecord> results
  ) {
  public static CachedConstraintRun fromJSON(JsonObject json) {
    return new CachedConstraintRun(
        json.getInt("constraint_id"),
        json.getInt("simulation_dataset_id"),
        json.getString("constraint_definition"),
        json.getBoolean("definition_outdated"),
        json.getJsonObject("results").isEmpty() ?
            Optional.empty() :
            Optional.of(ConstraintRecord.fromJSON(json.getJsonObject("results")))
    );
  }
}
