package gov.nasa.jpl.ammos.mpsa.aerie.plan.http;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.Breadcrumb;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.CreatedEntity;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ResponseSerializers {
  public static <T> JsonValue serializeList(final Function<T, JsonValue> elementSerializer, final List<T> elements) {
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

  public static JsonValue serializeString(final String value) {
    if (value == null) return JsonValue.NULL;
    return Json.createValue(value);
  }

  public static JsonValue serializeTimestamp(final Timestamp timestamp) {
    if (timestamp == null) return JsonValue.NULL;
    return Json.createValue(timestamp.toString());
  }

  public static JsonValue serializeStringList(final List<String> elements) {
    return serializeList(x -> serializeString(x), elements);
  }

  public static JsonValue serializeActivityParameter(final SerializedValue parameter) {
    if (parameter == null) return JsonValue.NULL;
    return parameter.match(new ParameterSerializationVisitor());
  }

  public static JsonValue serializeActivityParameterMap(final Map<String, SerializedValue> fields) {
    return serializeMap(x -> serializeActivityParameter(x), fields);
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
    return serializeMap(x -> serializeActivityInstance(x), fields);
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
    return serializeMap(x -> serializePlan(x), fields);
  }

  public static JsonValue serializeCreatedEntity(final CreatedEntity entity) {
    return Json.createObjectBuilder()
        .add("id", serializeString(entity.id))
        .build();
  }

  public static JsonValue serializeTimestamp(final Instant instant) {
    final var formattedTimestamp = DateTimeFormatter
        .ofPattern("uuuu-DDD'T'HH:mm:ss[.n]")
        .withZone(ZoneOffset.UTC)
        .format(instant);

    return Json.createValue(formattedTimestamp);
  }

  public static JsonValue serializeDuration(final Duration timestamp) {
    return Json.createValue(timestamp.dividedBy(Duration.MICROSECOND));
  }

  public static JsonValue serializeSimulationResults(final SimulationResults results) {
    if (results == null) return JsonValue.NULL;

    return Json.createObjectBuilder()
        .add("start", serializeTimestamp(results.startTime))
        .add("times", serializeList(element -> serializeDuration(element), results.timestamps))
        .add("resources", serializeMap(
            elements -> serializeList(element -> serializeActivityParameter(element), elements),
            results.timelines))
        .add("constraints", results.constraints)
        .build();
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

  public static JsonValue serializeValidationMessage(final List<Breadcrumb> breadcrumbs, final String message) {
    return Json.createObjectBuilder()
        .add("breadcrumbs", serializeList(ResponseSerializers::serializeBreadcrumb, breadcrumbs))
        .add("message", message)
        .build();
  }

  public static JsonValue serializeValidationMessages(final List<Pair<List<Breadcrumb>, String>> messages) {
    return serializeList(x -> serializeValidationMessage(x.getKey(), x.getValue()), messages);
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

  public static JsonValue serializeInvalidEntityException(final InvalidEntityException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "invalid json")
        .build();
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
      return serializeList(x -> x.match(this), elements);
    }
  }
}
