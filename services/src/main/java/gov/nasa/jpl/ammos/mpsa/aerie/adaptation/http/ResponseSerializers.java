package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.App;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.LocalApp;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.RemoteAdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.List;
import java.util.Map;

public final class ResponseSerializers {
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

  public static JsonValue serializeActivityParameter(final SerializedParameter parameter) {
    if (parameter == null) return JsonValue.NULL;

    return parameter.match(new ParameterSerializer());
  }

  public static JsonValue serializeActivityParameters(final Map<String, SerializedParameter> parameters) {
    if (parameters == null) return JsonValue.NULL;

    final JsonObjectBuilder builder = Json.createObjectBuilder();
    for (final var entry : parameters.entrySet()) {
      builder.add(entry.getKey(), serializeActivityParameter(entry.getValue()));
    }

    return builder.build();
  }

  public static JsonValue serializeActivityType(final ActivityType activityType) {
    return Json
        .createObjectBuilder()
        .add("parameters", serializeParameterSchemas(activityType.parameters))
        .add("defaults", serializeActivityParameters(activityType.defaults))
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

  public static JsonValue serializeAdaptation(final AdaptationJar adaptationJar) {
    return Json
        .createObjectBuilder()
        .add("name", adaptationJar.name == null ? JsonValue.NULL : Json.createValue(adaptationJar.name))
        .add("version", adaptationJar.version == null ? JsonValue.NULL : Json.createValue(adaptationJar.version))
        .add("mission", adaptationJar.mission == null ? JsonValue.NULL : Json.createValue(adaptationJar.mission))
        .add("owner", adaptationJar.owner == null ? JsonValue.NULL : Json.createValue(adaptationJar.owner))
        .build();
  }

  public static JsonValue serializeAdaptations(final Map<String, AdaptationJar> activityTypes) {
    if (activityTypes == null) return JsonValue.NULL;

    final JsonObjectBuilder builder = Json.createObjectBuilder();
    for (final var entry : activityTypes.entrySet()) {
      builder.add(entry.getKey(), serializeAdaptation(entry.getValue()));
    }
    return builder.build();
  }

  public static JsonValue serializeFailureList(final List<String> failures) {
    if (failures.size() > 0) {
      return Json.createObjectBuilder()
          .add("instantiable", JsonValue.FALSE)
          .add("failures", Json.createArrayBuilder(failures))
          .build();
    } else {
      return Json.createObjectBuilder()
          .add("instantiable", JsonValue.TRUE)
          .build();
    }
  }

  public static JsonValue serializedCreatedId(final String entityId) {
    return Json.createObjectBuilder()
        .add("id", entityId)
        .build();
  }

  static public JsonValue serializeParameter(final SerializedParameter parameter) {
    return parameter.match(new ParameterSerializer());
  }

  static public JsonValue serializeTimeline(final List<SerializedParameter> elements) {
    final var builder = Json.createArrayBuilder();
    for (final var element : elements) {
      builder.add(serializeParameter(element));
    }
    return builder.build();
  }

  static public JsonValue serializeTimestamp(final Instant timestamp) {
    final var duration = SimulationInstant.ORIGIN.durationTo(timestamp);
    return Json.createValue(duration.durationInMicroseconds);
  }

  static public JsonValue serializeTimestamps(final List<Instant> elements) {
    final var builder = Json.createArrayBuilder();
    for (final var element : elements) {
      builder.add(serializeTimestamp(element));
    }
    return builder.build();
  }

  static public JsonValue serializeSimulationResults(final SimulationResults results) {
    final var builder = Json.createObjectBuilder();
    builder.add("times", serializeTimestamps(results.timestamps));
    builder.add("resources", serializeMap(ResponseSerializers::serializeTimeline, results.timelines));
    return builder.build();
  }

  public static JsonValue serializeInvalidEntityException(final InvalidEntityException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "invalid json")
        .build();
  }

  public static JsonValue serializeValidationException(final ValidationException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "invalid entity")
        .add("failures", Json.createArrayBuilder(ex.getValidationErrors()))
        .build();
  }

  public static JsonValue serializeAdaptationRejectedException(final App.AdaptationRejectedException ex) {
    // TODO: Improve diagnostic information?
    return Json.createObjectBuilder()
        .add("message", "adaptation rejected: " + ex.getMessage())
        .build();
  }

  public static JsonValue serializeAdaptationContractException(final Adaptation.AdaptationContractException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", ex.getMessage())
        .build();
  }

  public static JsonValue serializeAdaptationLoadException(final LocalApp.AdaptationLoadException ex) {
    // TODO: Improve diagnostic information?
    return Json.createObjectBuilder()
        .add("message", ex.getMessage())
        .build();
  }

  public static JsonValue serializeAdaptationAccessException(final RemoteAdaptationRepository.AdaptationAccessException ex) {
    // TODO: Improve diagnostic information?
    return Json.createObjectBuilder()
        .add("message", ex.getMessage())
        .build();
  }

  private static final class ParameterSchemaSerializer implements ParameterSchema.Visitor<JsonValue> {
    @Override
    public JsonValue onReal() {
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

  private static final class ParameterSerializer implements SerializedParameter.Visitor<JsonValue> {
    @Override
    public JsonValue onNull() {
      return JsonValue.NULL;
    }

    @Override
    public JsonValue onReal(final double value) {
      return Json.createValue(value);
    }

    @Override
    public JsonValue onInt(final long value) {
      return Json.createValue(value);
    }

    @Override
    public JsonValue onBoolean(final boolean value) {
      return (value) ? JsonValue.TRUE : JsonValue.FALSE;
    }

    @Override
    public JsonValue onString(final String value) {
      return Json.createValue(value);
    }

    @Override
    public JsonValue onList(final List<SerializedParameter> elements) {
      final JsonArrayBuilder builder = Json.createArrayBuilder();
      for (final var element : elements) {
        builder.add(element.match(this));
      }
      return builder.build();
    }

    @Override
    public JsonValue onMap(final Map<String, SerializedParameter> fields) {
      final JsonObjectBuilder builder = Json.createObjectBuilder();
      for (final var entry : fields.entrySet()) {
        builder.add(entry.getKey(), entry.getValue().match(this));
      }
      return builder.build();
    }
  }
}
