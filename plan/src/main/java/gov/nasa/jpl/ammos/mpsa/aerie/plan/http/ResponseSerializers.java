package gov.nasa.jpl.ammos.mpsa.aerie.plan.http;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.Breadcrumb;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.CreatedEntity;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.bind.JsonbException;
import javax.json.stream.JsonParsingException;
import java.util.List;
import java.util.Map;

public final class ResponseSerializers {
  public static JsonValue serializeString(final String value) {
    if (value == null) return JsonValue.NULL;
    return Json.createValue(value);
  }

  public static JsonValue serializeStringList(final List<String> elements) {
    if (elements == null) return JsonValue.NULL;

    final var builder = Json.createArrayBuilder();
    for (final var element : elements) {
      builder.add(element);
    }

    return builder.build();
  }

  public static JsonValue serializeActivityParameter(final SerializedParameter parameter) {
    if (parameter == null) return JsonValue.NULL;
    return parameter.match(new ParameterSerializationVisitor());
  }

  public static JsonValue serializeActivityParameterMap(final Map<String, SerializedParameter> fields) {
    if (fields == null) return JsonValue.NULL;

    final var builder = Json.createObjectBuilder();
    for (final var field : fields.entrySet()) {
      builder.add(field.getKey(), serializeActivityParameter(field.getValue()));
    }

    return builder.build();
  }

  public static JsonValue serializeActivityInstance(final ActivityInstance activityInstance) {
    if (activityInstance == null) return JsonValue.NULL;

    return Json.createObjectBuilder()
        .add("type", serializeString(activityInstance.type))
        .add("startTimestamp", serializeString(activityInstance.startTimestamp))
        .add("parameters", serializeActivityParameterMap(activityInstance.parameters))
        .build();
  }

  public static JsonValue serializeActivityInstanceMap(final Map<String, ActivityInstance> fields) {
    final var builder = Json.createObjectBuilder();
    for (final var field : fields.entrySet()) {
      builder.add(field.getKey(), serializeActivityInstance(field.getValue()));
    }
    return builder.build();
  }

  public static JsonValue serializePlan(final Plan plan) {
    return Json.createObjectBuilder()
        .add("name", serializeString(plan.name))
        .add("adaptationId", serializeString(plan.adaptationId))
        .add("startTimestamp", serializeString(plan.startTimestamp))
        .add("endTimestamp", serializeString(plan.endTimestamp))
        .add("activityInstances", serializeActivityInstanceMap(plan.activityInstances))
        .build();
  }

  public static JsonValue serializePlanMap(final Map<String, Plan> fields) {
    final var builder = Json.createObjectBuilder();
    for (final var field : fields.entrySet()) {
      builder.add(field.getKey(), serializePlan(field.getValue()));
    }
    return builder.build();
  }

  public static JsonValue serializeCreatedEntity(final CreatedEntity entity) {
    return Json.createObjectBuilder()
        .add("id", serializeString(entity.id))
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
    final var breadcrumbsJson = Json.createArrayBuilder();
    for (final var breadcrumb : breadcrumbs) {
      breadcrumbsJson.add(serializeBreadcrumb(breadcrumb));
    }

    return Json.createObjectBuilder()
        .add("breadcrumbs", breadcrumbsJson)
        .add("message", message)
        .build();
  }

  public static JsonValue serializeValidationMessages(final List<Pair<List<Breadcrumb>, String>> messages) {
    final var messageJson = Json.createArrayBuilder();
    for (final var entry : messages) {
      messageJson.add(serializeValidationMessage(entry.getKey(), entry.getValue()));
    }

    return messageJson.build();
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

  public static JsonValue serializeJsonbException(final JsonbException ex) {
    return Json.createObjectBuilder()
        .add("message", "invalid json")
        .build();
  }

  static private class ParameterSerializationVisitor implements SerializedParameter.Visitor<JsonValue> {
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
    public JsonValue onMap(final Map<String, SerializedParameter> fields) {
      final var builder = Json.createObjectBuilder();
      for (final var field : fields.entrySet()) {
        builder.add(field.getKey(), field.getValue().match(this));
      }
      return builder.build();
    }

    @Override
    public JsonValue onList(final List<SerializedParameter> elements) {
      final var builder = Json.createArrayBuilder();
      for (final var element : elements) {
        builder.add(element.match(this));
      }
      return builder.build();
    }
  }
}
