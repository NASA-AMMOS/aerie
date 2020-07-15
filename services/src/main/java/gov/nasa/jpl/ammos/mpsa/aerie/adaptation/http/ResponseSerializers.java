package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.App;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.CreateSimulationMessage;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.LocalApp;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.RemoteAdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ResponseSerializers {
  public static <T> JsonValue serializeIterable(final Function<T, JsonValue> elementSerializer, final Iterable<T> elements) {
    if (elements == null) return JsonValue.NULL;

    final var builder = Json.createArrayBuilder();
    for (final var element : elements) builder.add(elementSerializer.apply(element));
    return builder.build();
  }

  public static <T> JsonValue serializeMap(final Function<T, JsonValue> fieldSerializer, final Map<String, T> fields) {
    if (fields == null) return JsonValue.NULL;

    final var builder = Json.createObjectBuilder();
    for (final var entry : fields.entrySet()) builder.add(entry.getKey(), fieldSerializer.apply(entry.getValue()));
    return builder.build();
  }

  public static JsonValue serializeParameterSchema(final ParameterSchema schema) {
    if (schema == null) return JsonValue.NULL;

    return schema.match(new ParameterSchemaSerializer());
  }

  public static JsonValue serializeParameterSchemas(final Map<String, ParameterSchema> schemas) {
    return serializeMap(ResponseSerializers::serializeParameterSchema, schemas);
  }

  public static JsonValue serializeActivityParameter(final SerializedParameter parameter) {
    if (parameter == null) return JsonValue.NULL;

    return parameter.match(new ParameterSerializer());
  }

  public static JsonValue serializeActivityParameters(final Map<String, SerializedParameter> parameters) {
    return serializeMap(ResponseSerializers::serializeActivityParameter, parameters);
  }

  public static JsonValue serializeActivityType(final ActivityType activityType) {
    return Json
        .createObjectBuilder()
        .add("parameters", serializeParameterSchemas(activityType.parameters))
        .add("defaults", serializeActivityParameters(activityType.defaults))
        .build();
  }

  public static JsonValue serializeActivityTypes(final Map<String, ActivityType> activityTypes) {
    return serializeMap(ResponseSerializers::serializeActivityType, activityTypes);
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
    return serializeMap(ResponseSerializers::serializeAdaptation, activityTypes);
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

  public static JsonValue serializeParameter(final SerializedParameter parameter) {
    return parameter.match(new ParameterSerializer());
  }

  public static JsonValue serializeTimeline(final List<SerializedParameter> elements) {
    return serializeIterable(ResponseSerializers::serializeParameter, elements);
  }

  public static JsonValue serializeTimestamp(final Duration timestamp) {
    return Json.createValue(timestamp.dividedBy(Duration.MICROSECOND));
  }

  public static JsonValue serializeTimestamps(final List<Duration> elements) {
    return serializeIterable(ResponseSerializers::serializeTimestamp, elements);
  }

  public static JsonValue serializeConstraintViolation(final ConstraintViolation violation) {
    return Json.createObjectBuilder()
            .add("associations", serializeConstraintViolationAssociations(violation))
            .add("constraint", serializeConstraintViolationMetadata(violation))
            .add("windows", serializeWindows(violation.violationWindows))
            .build();
  }

  public static JsonValue serializeConstraintViolationAssociations(ConstraintViolation violation) {
    return Json.createObjectBuilder()
            .add("activityInstanceIds", serializeIterable(Json::createValue, violation.associatedActivityIds))
            .add("stateIds", serializeIterable(Json::createValue, violation.associatedStateIds))
            .build();
  }

  public static JsonValue serializeConstraintViolationMetadata(ConstraintViolation violation) {
    return Json.createObjectBuilder()
            .add("id", violation.id)
            .add("name", violation.name)
            .add("message", violation.message)
            .add("category", violation.category)
            .build();
  }

  public static JsonValue serializeWindows(List<Window> windows) {
    return serializeIterable(ResponseSerializers::serializeWindow, windows);
  }

  public static JsonValue serializeWindow(final Window window) {
    return Json.createObjectBuilder()
            .add("start", window.start.dividedBy(Duration.MICROSECOND))
            .add("end", window.end.dividedBy(Duration.MICROSECOND))
            .build();
  }

  public static JsonValue serializeSimulationResults(final SimulationResults results) {
    if (results == null) return JsonValue.NULL;

    final var builder = Json.createObjectBuilder();
    builder.add("times", serializeTimestamps(results.timestamps));
    builder.add("resources", serializeMap(ResponseSerializers::serializeTimeline, results.timelines));
    builder.add("constraints", serializeIterable(ResponseSerializers::serializeConstraintViolation, results.constraintViolations));
    return builder.build();
  }

  public static JsonValue serializeScheduledActivity(final Pair<Duration, SerializedActivity> scheduledActivity) {
    return Json.createObjectBuilder()
        .add("defer", scheduledActivity.getLeft().dividedBy(Duration.MICROSECOND))
        .add("type", scheduledActivity.getRight().getTypeName())
        .add("parameters", serializeActivityParameters(scheduledActivity.getRight().getParameters()))
        .build();
  }

  public static JsonValue serializeScheduledActivities(final Map<String, Pair<Duration, SerializedActivity>> activities) {
    return serializeMap(ResponseSerializers::serializeScheduledActivity, activities);
  }

  public static JsonValue serializeCreateSimulationMessage(final CreateSimulationMessage message) {
    return Json.createObjectBuilder()
        .add("adaptationId", message.adaptationId)
        .add("startTime", DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss.nnnnnnnnn").withZone(ZoneOffset.UTC).format(message.startTime))
        .add("samplingDuration", message.samplingDuration.dividedBy(Duration.MICROSECOND))
        .add("samplingPeriod", message.samplingPeriod.dividedBy(Duration.MICROSECOND))
        .add("activities", serializeScheduledActivities(message.activityInstances))
        .build();
  }

  public static JsonValue serializeInvalidEntityException(final InvalidEntityException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("kind", "invalid-entity")
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
    public JsonValue onSequence(final ParameterSchema itemSchema) {
      return Json
          .createObjectBuilder()
          .add("type", "sequence")
          .add("items", itemSchema.match(this))
          .build();
    }

    @Override
    public JsonValue onStruct(final Map<String, ParameterSchema> parameterSchemas) {
      return Json
          .createObjectBuilder()
          .add("type", "struct")
          .add("items", serializeMap(x -> x.match(this), parameterSchemas))
          .build();
    }

    @Override
    public JsonValue onEnum(Class<? extends Enum<?>> enumeration) {
      var enumValues = Arrays.asList(enumeration.getEnumConstants());
      return Json
              .createObjectBuilder()
              .add("type", "enumerated")
              .add("items", serializeIterable(v -> Json.createObjectBuilder()
                      .add("key", v.name())
                      .add("label", v.toString())
                      .build(), enumValues))
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
      return serializeIterable(x -> x.match(this), elements);
    }

    @Override
    public JsonValue onMap(final Map<String, SerializedParameter> fields) {
      return serializeMap(x -> x.match(this), fields);
    }
  }
}
