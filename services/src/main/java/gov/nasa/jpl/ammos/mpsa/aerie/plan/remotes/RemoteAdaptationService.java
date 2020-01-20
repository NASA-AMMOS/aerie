package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.utils.HttpRequester;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

public final class RemoteAdaptationService implements AdaptationService {
  private final HttpRequester client;

  public RemoteAdaptationService(final URI serviceBaseUri) {
    this.client = new HttpRequester(HttpClient.newHttpClient(), serviceBaseUri);
  }

  private static ParameterSchema deserializeParameterSchema(final JsonValue parameterSchemaJsonValue) {
    if (!(parameterSchemaJsonValue instanceof JsonObject)) throw new InvalidServiceResponseException();
    final JsonObject parameterSchemaJson = parameterSchemaJsonValue.asJsonObject();

    if (!(parameterSchemaJson.get("type") instanceof JsonString)) throw new InvalidServiceResponseException();
    final String parameterType = parameterSchemaJson.getString("type");

    switch (parameterType) {
      case "string":
        return ParameterSchema.STRING;
      case "int":
        return ParameterSchema.INT;
      case "bool":
        return ParameterSchema.BOOLEAN;
      case "double":
        return ParameterSchema.REAL;
      case "list":
        if (!(parameterSchemaJson.containsKey("items"))) throw new InvalidServiceResponseException();

        return ParameterSchema.ofList(deserializeParameterSchema(parameterSchemaJson.get("items")));
      case "map":
        if (!(parameterSchemaJson.containsKey("items"))) throw new InvalidServiceResponseException();
        if (!(parameterSchemaJson.get("items") instanceof JsonObject)) throw new InvalidServiceResponseException();

        final Map<String, ParameterSchema> fieldSchemas = new HashMap<>();
        for (final var entry : parameterSchemaJson.get("items").asJsonObject().entrySet()) {
          fieldSchemas.put(entry.getKey(), deserializeParameterSchema(entry.getValue()));
        }

        return ParameterSchema.ofMap(fieldSchemas);
      default:
        throw new InvalidServiceResponseException();
    }
  }

  private static Map<String, Map<String, ParameterSchema>> deserializeActivityTypes(final JsonValue activityTypesJson) {
    if (!(activityTypesJson instanceof JsonObject)) throw new InvalidServiceResponseException();

    final Map<String, Map<String, ParameterSchema>> activityTypes = new HashMap<>();
    for (final var activityTypeEntry : activityTypesJson.asJsonObject().entrySet()) {
      final String activityTypeName = activityTypeEntry.getKey();

      if (!(activityTypeEntry.getValue() instanceof JsonObject)) throw new InvalidServiceResponseException();
      final JsonObject activityTypeJson = activityTypeEntry.getValue().asJsonObject();

      if (!activityTypeJson.containsKey("parameters")) throw new InvalidServiceResponseException();
      if (!(activityTypeJson.get("parameters") instanceof JsonObject)) throw new InvalidServiceResponseException();
      final JsonObject parameterSchemasJson = activityTypeJson.get("parameters").asJsonObject();

      final Map<String, ParameterSchema> parameterSchemas = new HashMap<>();
      for (final var parameterSchemaEntry : parameterSchemasJson.entrySet()) {
        parameterSchemas.put(parameterSchemaEntry.getKey(), deserializeParameterSchema(parameterSchemaEntry.getValue()));
      }

      activityTypes.put(activityTypeName, parameterSchemas);
    }

    return activityTypes;
  }

  public static class InvalidServiceResponseException extends RuntimeException {
  }

  private static class AdaptationAccessException extends RuntimeException {
    public AdaptationAccessException(final Throwable cause) {
      super(cause);
    }

    public AdaptationAccessException(final String message) {
      super(message);
    }
  }
}
