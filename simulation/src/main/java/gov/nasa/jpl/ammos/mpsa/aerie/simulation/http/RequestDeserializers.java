package gov.nasa.jpl.ammos.mpsa.aerie.simulation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.Breadcrumb;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.InvalidEntityFailure;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RequestDeserializers {
  private RequestDeserializers() {}

  static public Result<String, List<InvalidEntityFailure>> deserializeString(final JsonValue jsonValue) {
    if (!(jsonValue instanceof JsonString)) {
      return Result.failure(List.of(InvalidEntityFailure.message("is expected to be a string")));
    }
    final JsonString stringJson = (JsonString) jsonValue;

    return Result.success(stringJson.getString());
  }

  static public Result<CreateSimulationMessage, List<InvalidEntityFailure>> deserializeCreateSimulationMessage(final String subject) {
    final JsonValue requestJson = Json.createReader(new StringReader(subject)).readValue();
    if (!(requestJson instanceof JsonObject)) {
      return Result.failure(List.of(InvalidEntityFailure.message("is expected to be an object")));
    }

    final List<InvalidEntityFailure> failures = new ArrayList<>();

    // Extract all fields from the JSON subject.
    Optional<Result<String, List<InvalidEntityFailure>>> planId = Optional.empty();
    for (final var entry : ((JsonObject)requestJson).entrySet()) {
      switch (entry.getKey()) {
        case "planId":
          planId = Optional.of(deserializeString(entry.getValue()));
          break;
        default:
          failures.add(InvalidEntityFailure.scope(Breadcrumb.of(entry.getKey()), List.of(InvalidEntityFailure.message("is an unknown key"))));
          break;
      }
    }

    // If any errors occurred, collect them and bail out.
    if (planId.isEmpty()) {
      failures.add(InvalidEntityFailure.scope(Breadcrumb.of("planId"), List.of(InvalidEntityFailure.message("is required"))));
    } else if (planId.get().getKind() == Result.Kind.Failure) {
      // SAFETY: `planId.get()` must be a Failure variant.
      failures.add(InvalidEntityFailure.scope(Breadcrumb.of("planId"), planId.get().getFailureOrThrow()));
    }

    if (failures.size() > 0) return Result.failure(failures);

    // Instantiate the entity with all retrieved fields.
    // SAFETY: `planId.get()` must be a Success variant.
    final String planIdField = planId.get().getSuccessOrThrow();

    return Result.success(new CreateSimulationMessage(planIdField));
  }
}
