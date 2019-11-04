package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.Map;

public class ResponseSerializers {
  public static JsonValue serializeParameterSchema(final ParameterSchema schema) {
    if (schema == null) return JsonValue.NULL;

    return schema.match(new ParameterSchemaSerializer());
  }

  public static JsonValue serializeParameterSchemas(final Map<String, ParameterSchema> schemas) {
    if (schemas == null) return JsonValue.NULL;

    final JsonObjectBuilder builder = Json.createObjectBuilder();
    for (final var entry : schemas.entrySet()) {
      builder.add(entry.getKey(), serializeParameterSchema(entry.getValue()));
    }
    return builder.build();
  }

  public static JsonValue serializeActivityType(final ActivityType activityType) {
    return Json
        .createObjectBuilder()
        .add("parameters", serializeParameterSchemas(activityType.parameters))
        .build();
  }

  public static JsonValue serializeActivityTypes(final Map<String, ActivityType> activityTypes) {
    if (activityTypes == null) return JsonValue.NULL;

    final JsonObjectBuilder builder = Json.createObjectBuilder();
    for (final var entry : activityTypes.entrySet()) {
      builder.add(entry.getKey(), serializeActivityType(entry.getValue()));
    }
    return builder.build();
  }

  public static JsonValue serializeAdaptation(final Adaptation adaptation) {
    return Json
        .createObjectBuilder()
        .add("name", adaptation.name == null ? JsonValue.NULL : Json.createValue(adaptation.name))
        .add("version", adaptation.version == null ? JsonValue.NULL : Json.createValue(adaptation.version))
        .add("mission", adaptation.mission == null ? JsonValue.NULL : Json.createValue(adaptation.mission))
        .add("owner", adaptation.owner == null ? JsonValue.NULL : Json.createValue(adaptation.owner))
        .build();
  }

  public static JsonValue serializeAdaptations(final Map<String, Adaptation> activityTypes) {
    if (activityTypes == null) return JsonValue.NULL;

    final JsonObjectBuilder builder = Json.createObjectBuilder();
    for (final var entry : activityTypes.entrySet()) {
      builder.add(entry.getKey(), serializeAdaptation(entry.getValue()));
    }
    return builder.build();
  }

  public static JsonValue serializedCreatedId(final String entityId) {
    return Json.createObjectBuilder()
        .add("id", entityId)
        .build();
  }

  private static final class ParameterSchemaSerializer implements ParameterSchema.Visitor<JsonValue> {
    @Override
    public JsonValue onDouble() {
      return Json
          .createObjectBuilder()
          .add("type", "double")
          .build();
    }

    @Override
    public JsonValue onInt() {
      return Json
          .createObjectBuilder()
          .add("type", "int")
          .build();
    }

    @Override
    public JsonValue onBoolean() {
      return Json
          .createObjectBuilder()
          .add("type", "bool")
          .build();
    }

    @Override
    public JsonValue onString() {
      return Json
          .createObjectBuilder()
          .add("type", "string")
          .build();
    }

    @Override
    public JsonValue onList(final ParameterSchema itemSchema) {
      return Json
          .createObjectBuilder()
          .add("type", "list")
          .add("items", itemSchema.match(this))
          .build();
    }

    @Override
    public JsonValue onMap(final Map<String, ParameterSchema> parameterSchemas) {
      final JsonObjectBuilder builder = Json.createObjectBuilder();
      for (final var entry : parameterSchemas.entrySet()) {
        builder.add(entry.getKey(), entry.getValue().match(this));
      }

      return Json
          .createObjectBuilder()
          .add("type", "map")
          .add("items", builder)
          .build();
    }
  }
}
