package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.App;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.CreateSimulationMessage;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.LocalApp;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationFacade;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.json.Breadcrumb;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.RemoteAdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConditionTypes;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintStructure;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintStructure.ConstraintStructureVisitor;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.SimulatedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;
import gov.nasa.jpl.ammos.mpsa.aerie.json.JsonParseResult.FailureReason;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ResponseSerializers {
  public static <T> JsonValue serializeIterable(
      final Function<T, JsonValue> elementSerializer,
      final Iterable<T> elements)
  {
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

  public static <T> JsonValue serializeNullable(final Function<T, JsonValue> serializer, final T value) {
    if (value != null) return serializer.apply(value);
    else return JsonValue.NULL;
  }

  public static JsonValue serializeParameterSchema(final ValueSchema schema) {
    if (schema == null) return JsonValue.NULL;

    return schema.match(new ValueSchemaSerializer());
  }

  public static JsonValue serializeParameterSchemas(final Map<String, ValueSchema> schemas) {
    return serializeMap(ResponseSerializers::serializeParameterSchema, schemas);
  }

  public static JsonValue serializeActivityParameter(final SerializedValue parameter) {
    if (parameter == null) return JsonValue.NULL;

    return parameter.match(new ParameterSerializer());
  }

  public static JsonValue serializeActivityParameters(final Map<String, SerializedValue> parameters) {
    return serializeMap(ResponseSerializers::serializeActivityParameter, parameters);
  }

  public static JsonValue serializeConstraintStructure(ConstraintStructure structure) {

    return structure.visit(new ConstraintStructureVisitor<>() {

      @Override
      public JsonValue onActivityConstraintStructure(
          final String activityType,
          final ConditionTypes.ActivityCondition condition)
      {
        return Json.createObjectBuilder()
                   .add("constraint type", "Activity")
                   .add("activity type", activityType)
                   .add("condition", condition.toString())
                   .build();
      }

      @Override
      public JsonValue onStateConstraintStructure(
          final String stateName,
          final ConditionTypes.StateComparator comparator,
          final SerializedValue value)
      {
        return Json.createObjectBuilder()
                   .add("constraint type", "State")
                   .add("state name", stateName)
                   .add("comparator", comparator.toString())
                   .add("value", serializeParameter(value))
                   .build();
      }

      @Override
      public JsonValue onComplexConstraintStructure(
          final ConditionTypes.Connector connector,
          final ConstraintStructure left,
          final ConstraintStructure right)
      {
        return Json.createObjectBuilder()
                   .add("constraint type", "Complex")
                   .add("connector", connector.toString())
                   .add("left", serializeConstraintStructure(left))
                   .add("right", serializeConstraintStructure(right))
                   .build();
      }
    });
  }

  public static JsonValue serializeConstraintType(final ViolableConstraint constraintType) {
    return Json
        .createObjectBuilder()
        .add("name", constraintType.name)
        .add("message", constraintType.message)
        .add("category", constraintType.category)
        .add("stateIDs", Json.createArrayBuilder(constraintType.getStateIds()))
        .add("activityTypes", Json.createArrayBuilder(constraintType.getActivityTypes()))
        .add("structure", serializeConstraintStructure(constraintType.getStructure())).build();
  }

  public static JsonValue serializeConstraintTypes(List<ViolableConstraint> constraintTypes) {
    return serializeIterable(ResponseSerializers::serializeConstraintType, constraintTypes);
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

  public static JsonValue serializeParameter(final SerializedValue parameter) {
    return parameter.match(new ParameterSerializer());
  }

  public static JsonValue serializeTimeline(final List<SerializedValue> elements) {
    return serializeIterable(ResponseSerializers::serializeParameter, elements);
  }

  public static JsonValue serializeTimestamp(final Duration timestamp) {
    return Json.createValue(timestamp.in(Duration.MICROSECONDS));
  }

  public static JsonValue serializeTimestamps(final List<Duration> elements) {
    return serializeIterable(ResponseSerializers::serializeTimestamp, elements);
  }

  public static JsonValue serializeTimestampString(final TemporalAccessor timestamp) {
    return Json.createValue(
        DateTimeFormatter
            .ofPattern("uuuu-DDD'T'HH:mm:ss.SSSSSS")
            .withZone(ZoneOffset.UTC)
            .format(timestamp));
  }

  public static JsonValue serializeDuration(final Duration duration) {
    return Json.createValue(duration.in(Duration.MICROSECONDS));
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

  public static JsonValue serializeStatesSchemas(Map<String, ValueSchema> schemaMap) {
    return serializeMap(ResponseSerializers::serializeParameterSchema, schemaMap);
  }

  public static JsonValue serializeWindows(Windows windows) {
    return serializeIterable(ResponseSerializers::serializeWindow, windows);
  }

  public static JsonValue serializeWindow(final Window window) {
    return Json.createObjectBuilder()
               .add("start", window.start.in(Duration.MICROSECONDS))
               .add("end", window.end.in(Duration.MICROSECONDS))
               .build();
  }

  public static JsonValue serializeSimulationResults(final SimulationResults results) {
    if (results == null) return JsonValue.NULL;

    final var builder = Json.createObjectBuilder();
    builder.add("times", serializeTimestamps(results.timestamps));
    builder.add("resources", serializeMap(ResponseSerializers::serializeTimeline, results.timelines));
    builder.add(
        "constraints",
        serializeIterable(ResponseSerializers::serializeConstraintViolation, results.constraintViolations));
    builder.add("activities", serializeSimulatedActivities(results.simulatedActivities));
    return builder.build();
  }

  public static JsonValue serializeSimulatedActivities(final Map<String, SimulatedActivity> simulatedActivities) {
    return serializeMap(ResponseSerializers::serializeSimulatedActivity, simulatedActivities);
  }

  public static JsonValue serializeSimulatedActivity(final SimulatedActivity simulatedActivity) {
    return Json.createObjectBuilder()
               .add("type", simulatedActivity.type)
               .add("parameters", serializeActivityParameters(simulatedActivity.parameters))
               .add("startTimestamp", serializeTimestampString(simulatedActivity.start))
               .add("duration", serializeDuration(simulatedActivity.duration))
               .add("parent", serializeNullable(Json::createValue, simulatedActivity.parentId))
               .add("children", serializeIterable(Json::createValue, simulatedActivity.childIds))
               .build();
  }

  public static JsonValue serializeScheduledActivity(final Pair<Duration, SerializedActivity> scheduledActivity) {
    return Json.createObjectBuilder()
               .add("defer", scheduledActivity.getLeft().in(Duration.MICROSECONDS))
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
               .add("startTime", serializeTimestampString(message.startTime))
               .add("samplingDuration", message.samplingDuration.in(Duration.MICROSECONDS))
               .add("samplingPeriod", message.samplingPeriod.in(Duration.MICROSECONDS))
               .add("activities", serializeScheduledActivities(message.activityInstances))
               .build();
  }

  public static JsonValue serializeInvalidJsonException(final InvalidJsonException ex) {
    return Json.createObjectBuilder()
               .add("kind", "invalid-entity")
               .add("message", "invalid json")
               .build();
  }

  public static JsonValue serializeInvalidEntityException(final InvalidEntityException ex) {
    return Json.createObjectBuilder()
               .add("kind", "invalid-entity")
               .add("failures", serializeIterable(ResponseSerializers::serializeFailureReason, ex.failures))
               .build();
  }

  public static JsonValue serializeFailureReason(final FailureReason failure) {
    return Json.createObjectBuilder()
               .add("breadcrumbs", serializeIterable(ResponseSerializers::serializeBreadcrumb, failure.breadcrumbs))
               .add("message", failure.reason)
               .build();
  }

  public static JsonValue serializeBreadcrumb(final Breadcrumb breadcrumb) {
    return breadcrumb.visit(new Breadcrumb.BreadcrumbVisitor<>() {
      @Override
      public JsonValue onString(final String s) {
        return Json.createValue(s);
      }

      @Override
      public JsonValue onInteger(final Integer i) {
        return Json.createValue(i);
      }
    });
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

  public static JsonValue serializeAdaptationContractException(final AdaptationFacade.AdaptationContractException ex) {
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

  public static JsonValue serializeTaskSpecInstantiationException(final SimulationDriver.TaskSpecInstantiationException ex) {
    // TODO: Improve diagnostic information?
    return Json.createObjectBuilder()
        .add("message", ex.getMessage())
        .add("activityId", ex.id)
        .build();
  }

  private static final class ValueSchemaSerializer implements ValueSchema.Visitor<JsonValue> {
    @Override
    public JsonValue onReal() {
      return Json
          .createObjectBuilder()
          .add("type", "real")
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
          .add("type", "boolean")
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
    public JsonValue onDuration() {
      return Json
          .createObjectBuilder()
          .add("type", "duration")
          .build();
    }

    @Override
    public JsonValue onSeries(final ValueSchema itemSchema) {
      return Json
          .createObjectBuilder()
          .add("type", "series")
          .add("items", itemSchema.match(this))
          .build();
    }

    @Override
    public JsonValue onStruct(final Map<String, ValueSchema> parameterSchemas) {
      return Json
          .createObjectBuilder()
          .add("type", "struct")
          .add("items", serializeMap(x -> x.match(this), parameterSchemas))
          .build();
    }

    @Override
    public JsonValue onVariant(Class<? extends Enum<?>> enumeration) {
      var enumValues = Arrays.asList(enumeration.getEnumConstants());
      return Json
          .createObjectBuilder()
          .add("type", "variant")
          .add("variants", serializeIterable(v -> Json.createObjectBuilder()
                                                      .add("key", v.name())
                                                      .add("label", v.toString())
                                                      .build(), enumValues))
          .build();
    }
  }

  private static final class ParameterSerializer implements SerializedValue.Visitor<JsonValue> {
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
    public JsonValue onList(final List<SerializedValue> elements) {
      return serializeIterable(x -> x.match(this), elements);
    }

    @Override
    public JsonValue onMap(final Map<String, SerializedValue> fields) {
      return serializeMap(x -> x.match(this), fields);
    }
  }
}
