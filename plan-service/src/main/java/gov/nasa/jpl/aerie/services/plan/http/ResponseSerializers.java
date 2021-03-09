package gov.nasa.jpl.aerie.services.plan.http;

import gov.nasa.jpl.aerie.json.JsonParseResult.FailureReason;
import gov.nasa.jpl.aerie.merlin.driver.ConstraintViolation;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.ViolableConstraint;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.aerie.services.adaptation.app.CreateSimulationMessage;
import gov.nasa.jpl.aerie.services.adaptation.models.ActivityType;
import gov.nasa.jpl.aerie.services.adaptation.models.AdaptationFacade;
import gov.nasa.jpl.aerie.services.adaptation.models.AdaptationJar;
import gov.nasa.jpl.aerie.services.adaptation.remotes.RemoteAdaptationRepository;
import gov.nasa.jpl.aerie.services.plan.controllers.Breadcrumb;
import gov.nasa.jpl.aerie.services.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.services.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.services.plan.exceptions.ValidationException;
import gov.nasa.jpl.aerie.services.plan.models.ActivityInstance;
import gov.nasa.jpl.aerie.services.plan.models.CreatedEntity;
import gov.nasa.jpl.aerie.services.plan.models.Plan;
import gov.nasa.jpl.aerie.services.plan.models.Timestamp;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ResponseSerializers {
  public static <T> JsonValue serializeNullable(final Function<T, JsonValue> serializer, final T value) {
    if (value != null) return serializer.apply(value);
    else return JsonValue.NULL;
  }

  public static <T> JsonValue
  serializeIterable(final Function<T, JsonValue> elementSerializer, final Iterable<T> elements) {
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

  public static JsonValue serializeParameterSchema(final ValueSchema schema) {
    if (schema == null) return JsonValue.NULL;

    return schema.match(new ValueSchemaSerializer());
  }

  public static JsonValue serializeParameterSchemas(final Map<String, ValueSchema> schemas) {
    return serializeMap(ResponseSerializers::serializeParameterSchema, schemas);
  }

  public static JsonValue serializeSample(final Pair<Duration, SerializedValue> element) {
    if (element == null) return JsonValue.NULL;
    return Json
        .createObjectBuilder()
        .add("x", serializeDuration(element.getLeft()))
        .add("y", serializeActivityParameter(element.getRight()))
        .build();
  }

  public static JsonValue serializeString(final String value) {
    if (value == null) return JsonValue.NULL;
    return Json.createValue(value);
  }

  public static JsonValue serializeTimestamp(final Timestamp timestamp) {
    if (timestamp == null) return JsonValue.NULL;
    return Json.createValue(timestamp.toString());
  }

  public static JsonValue serializeStringList(final List<String> elements) {
    return serializeIterable(ResponseSerializers::serializeString, elements);
  }

  public static JsonValue serializeActivityParameter(final SerializedValue parameter) {
    if (parameter == null) return JsonValue.NULL;
    return parameter.match(new ParameterSerializationVisitor());
  }

  public static JsonValue serializeActivityParameterMap(final Map<String, SerializedValue> fields) {
    return serializeMap(ResponseSerializers::serializeActivityParameter, fields);
  }

  public static JsonValue serializeActivityInstance(final ActivityInstance activityInstance) {
    if (activityInstance == null) return JsonValue.NULL;

    return Json.createObjectBuilder()
        .add("type", serializeString(activityInstance.type))
        .add("startTimestamp", serializeTimestamp(activityInstance.startTimestamp))
        .add("parameters", serializeActivityParameterMap(activityInstance.parameters))
        .build();
  }

  public static JsonValue serializeActivityInstanceMap(final Map<String, ActivityInstance> fields) {
    return serializeMap(ResponseSerializers::serializeActivityInstance, fields);
  }

  public static JsonValue serializedCreatedId(final String entityId) {
    return Json.createObjectBuilder()
               .add("id", entityId)
               .build();
  }

  public static JsonValue serializePlan(final Plan plan) {
    return Json.createObjectBuilder()
        .add("name", serializeString(plan.name))
        .add("adaptationId", serializeString(plan.adaptationId))
        .add("startTimestamp", serializeTimestamp(plan.startTimestamp))
        .add("endTimestamp", serializeTimestamp(plan.endTimestamp))
        .add("activityInstances", serializeActivityInstanceMap(plan.activityInstances))
        .build();
  }

  public static JsonValue serializePlanMap(final Map<String, Plan> fields) {
    return serializeMap(ResponseSerializers::serializePlan, fields);
  }

  public static JsonValue serializeConstraintViolation(final ConstraintViolation violation) {
    return Json
        .createObjectBuilder()
        .add("associations", Json
            .createObjectBuilder()
            .add("activityInstanceIds", serializeIterable(Json::createValue, violation.associatedActivityIds))
            .add("stateIds", serializeIterable(Json::createValue, violation.associatedStateIds))
            .build())
        .add("constraint", Json
            .createObjectBuilder()
            .add("id", violation.id)
            .add("name", violation.name)
            .add("message", violation.message)
            .add("category", violation.category)
            .build())
        .add("windows", serializeIterable(ResponseSerializers::serializeWindow, violation.violationWindows))
        .build();
  }

  public static JsonValue serializeStatesSchemas(Map<String, ValueSchema> schemaMap) {
    return serializeMap(gov.nasa.jpl.aerie.services.plan.http.ResponseSerializers::serializeParameterSchema, schemaMap);
  }

  public static JsonValue serializeWindow(final Window window) {
    return Json.createObjectBuilder()
               .add("start", window.start.in(Duration.MICROSECONDS))
               .add("end", window.end.in(Duration.MICROSECONDS))
               .build();
  }

  public static JsonValue serializeSimulatedActivity(final SimulatedActivity simulatedActivity) {
    return Json
        .createObjectBuilder()
        .add("type", simulatedActivity.type)
        .add("parameters", serializeActivityParameterMap(simulatedActivity.parameters))
        .add("startTimestamp", serializeTimestamp(simulatedActivity.start))
        .add("duration", serializeDuration(simulatedActivity.duration))
        .add("parent", serializeNullable(Json::createValue, simulatedActivity.parentId))
        .add("children", serializeIterable(Json::createValue, simulatedActivity.childIds))
        .build();
  }

  public static JsonValue serializeSimulationResults(final SimulationResults results) {
    if (results == null) return JsonValue.NULL;

    return Json
        .createObjectBuilder()
        .add("start", serializeTimestamp(results.startTime))
        .add("resources", serializeMap(
            elements -> serializeIterable(gov.nasa.jpl.aerie.services.plan.http.ResponseSerializers::serializeSample, elements),
            results.resourceSamples))
        .add("constraints", serializeIterable(ResponseSerializers::serializeConstraintViolation, results.constraintViolations))
        .add("activities", serializeMap(ResponseSerializers::serializeSimulatedActivity, results.simulatedActivities))
        .build();
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

  public static JsonValue serializeCreatedEntity(final CreatedEntity entity) {
    return Json.createObjectBuilder()
        .add("id", serializeString(entity.id))
        .build();
  }

  public static JsonValue serializeTimestamp(final TemporalAccessor instant) {
    final var formattedTimestamp = DateTimeFormatter
        .ofPattern("uuuu-DDD'T'HH:mm:ss.SSSSSS")
        .withZone(ZoneOffset.UTC)
        .format(instant);

    return Json.createValue(formattedTimestamp);
  }

  public static JsonValue serializeDuration(final Duration timestamp) {
    return Json.createValue(timestamp.in(Duration.MICROSECONDS));
  }

  public static JsonValue serializeActivityType(final ActivityType activityType) {
    return Json
        .createObjectBuilder()
        .add("parameters", gov.nasa.jpl.aerie.services.plan.http.ResponseSerializers.serializeParameterSchemas(activityType.parameters))
        .add("defaults", serializeActivityParameterMap(activityType.defaults))
        .build();
  }

  public static JsonValue serializeActivityTypes(final Map<String, ActivityType> activityTypes) {
    return serializeMap(ResponseSerializers::serializeActivityType, activityTypes);
  }

  public static JsonValue serializeConstraintType(final ViolableConstraint constraintType) {
    return Json
        .createObjectBuilder()
        .add("name", constraintType.name)
        .add("message", constraintType.message)
        .add("category", constraintType.category)
        .add("stateIDs", serializeIterable(Json::createValue, constraintType.stateIds))
        .add("activityTypes", serializeIterable(Json::createValue, constraintType.activityTypes))
        .build();
  }

  public static JsonValue serializeConstraintTypes(List<ViolableConstraint> constraintTypes) {
    return serializeIterable(ResponseSerializers::serializeConstraintType, constraintTypes);
  }

  private static JsonValue serializeBreadcrumb(final Breadcrumb breadcrumb) {
    return breadcrumb.match(new Breadcrumb.Visitor<>() {
      @Override
      public JsonValue onListIndex(final int index) {
        return Json.createValue(index);
      }

      @Override
      public JsonValue onMapIndex(final String index) {
        return Json.createValue(index);
      }
    });
  }

  public static JsonValue serializeScheduledActivity(final Pair<Duration, SerializedActivity> scheduledActivity) {
    return Json.createObjectBuilder()
               .add("defer", scheduledActivity.getLeft().in(Duration.MICROSECONDS))
               .add("type", scheduledActivity.getRight().getTypeName())
               .add("parameters", serializeActivityParameterMap(scheduledActivity.getRight().getParameters()))
               .build();
  }

  public static JsonValue serializeScheduledActivities(final Map<String, Pair<Duration, SerializedActivity>> activities) {
    return serializeMap(ResponseSerializers::serializeScheduledActivity, activities);
  }

  public static JsonValue serializeCreateSimulationMessage(final CreateSimulationMessage message) {
    return Json.createObjectBuilder()
               .add("adaptationId", message.adaptationId)
               .add("startTime", serializeTimestamp(message.startTime))
               .add("samplingDuration", message.samplingDuration.in(Duration.MICROSECONDS))
               .add("activities", serializeScheduledActivities(message.activityInstances))
               .build();
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

  public static JsonValue serializeValidationMessage(final List<Breadcrumb> breadcrumbs, final String message) {
    return Json.createObjectBuilder()
        .add("breadcrumbs", serializeIterable(ResponseSerializers::serializeBreadcrumb, breadcrumbs))
        .add("message", message)
        .build();
  }

  public static JsonValue serializeValidationMessages(final List<Pair<List<Breadcrumb>, String>> messages) {
    return serializeIterable(x -> serializeValidationMessage(x.getKey(), x.getValue()), messages);
  }

  public static JsonValue serializeValidationException(final ValidationException ex) {
    return serializeValidationMessages(ex.getValidationErrors());
  }

  public static JsonValue serializeJsonParsingException(final JsonParsingException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "invalid json")
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

  public static JsonValue serializeValidationException(final NewAdaptationValidationException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
               .add("message", "invalid entity")
               .add("failures", Json.createArrayBuilder(ex.getValidationErrors()))
               .build();
  }

  public static JsonValue serializeAdaptationRejectedException(
      final gov.nasa.jpl.aerie.services.adaptation.app.App.AdaptationRejectedException ex)
  {
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

  public static JsonValue serializeAdaptationLoadException(
      final gov.nasa.jpl.aerie.services.adaptation.app.LocalApp.AdaptationLoadException ex)
  {
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

  public static JsonValue serializeFailureReason(final FailureReason failure) {
    return Json.createObjectBuilder()
               .add("breadcrumbs", serializeIterable(ResponseSerializers::serializeParseFailureBreadcrumb, failure.breadcrumbs))
               .add("message", failure.reason)
               .build();
  }

  public static JsonValue serializeParseFailureBreadcrumb(final gov.nasa.jpl.aerie.json.Breadcrumb breadcrumb) {
    return breadcrumb.visit(new gov.nasa.jpl.aerie.json.Breadcrumb.BreadcrumbVisitor<>() {
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

  public static JsonValue serializeNoSuchPlanException(final NoSuchPlanException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "no such plan")
        .build();
  }

  public static JsonValue serializeNoSuchActivityInstanceException(final NoSuchActivityInstanceException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "no such activity instance")
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

  static private class ParameterSerializationVisitor implements SerializedValue.Visitor<JsonValue> {
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
    public JsonValue onMap(final Map<String, SerializedValue> fields) {
      return serializeMap(x -> x.match(this), fields);
    }

    @Override
    public JsonValue onList(final List<SerializedValue> elements) {
      return serializeIterable(x -> x.match(this), elements);
    }
  }
}
