package gov.nasa.jpl.aerie.scheduler.server.http;

import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.server.services.UnexpectedSubtypeError;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleAction;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleResults;

import javax.json.Json;
import javax.json.JsonValue;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * json serialization methods for data entities used in the scheduler response bodies
 */
public class ResponseSerializers {

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

  /**
   * serialize the scheduler run result, including if it is incomplete/failed
   *
   * @param response the result of the scheduling run to serialize
   * @return a json serialization of the scheduling run result
   */
  public static JsonValue serializeScheduleResultsResponse(final ScheduleAction.Response response) {
    if (response instanceof ScheduleAction.Response.Incomplete) {
      return Json
          .createObjectBuilder()
          .add("status", "incomplete")
          .build();
    } else if (response instanceof ScheduleAction.Response.Failed r) {
      return Json
          .createObjectBuilder()
          .add("status", "failed")
          .add("reason", r.reason())
          .build();
    } else if (response instanceof ScheduleAction.Response.Complete r) {
      return Json
          .createObjectBuilder()
          .add("status", "complete")
          .add("results", serializeScheduleResults(r.results()))
          .build();
    } else {
      throw new UnexpectedSubtypeError(ScheduleAction.Response.class, response);
    }
  }

  /**
   * serialize the provided scheduling result summary to json
   *
   * @param results the scheduling results to serialize
   * @return a json serialization of the given scheduling result
   */
  public static JsonValue serializeScheduleResults(final ScheduleResults results)
  {
    return serializeMap(
        ResponseSerializers::serializeGoalResult,
        results.goalResults()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(e -> Long.toString(e.getKey().id()), Map.Entry::getValue)));
  }

  private static JsonValue serializeGoalResult(final ScheduleResults.GoalResult goalResult) {
    return Json
        .createObjectBuilder()
        .add("createdActivities", serializeIterable(
            id -> Json.createValue(id.id()),
            goalResult.createdActivities()))
        .add("satisfyingActivities", serializeIterable(
            id -> Json.createValue(id.id()),
            goalResult.satisfyingActivities()))
        .add("createdActivities", goalResult.satisfied())
        .build();
  }

  /**
   * create report of given exception that can be passed as json payload
   *
   * @param e the exception to generate json report for
   * @return a json serialization of the exception details
   */
  public static JsonValue serializeException(final Exception e) {
    //TODO: stack trace or other details back to ui / client?
    return Json.createObjectBuilder()
               .add("message", e.toString())
               .build();
  }

  public static JsonValue serializeFailureReason(final JsonParseResult.FailureReason failure) {
    return Json.createObjectBuilder()
               .add("breadcrumbs", serializeIterable(ResponseSerializers::serializeParseFailureBreadcrumb, failure.breadcrumbs()))
               .add("message", failure.reason())
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

  public static JsonValue serializeValueSchema(final ValueSchema schema) {
    if (schema == null) return JsonValue.NULL;

    return schema.match(new ValueSchemaSerializer());
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
    public JsonValue onPath() {
      return Json
          .createObjectBuilder()
          .add("type", "path")
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
    public JsonValue onVariant(final List<ValueSchema.Variant> variants) {
      return Json
          .createObjectBuilder()
          .add("type", "variant")
          .add("variants", serializeIterable(
              v -> Json
                  .createObjectBuilder()
                  .add("key", v.key())
                  .add("label", v.label())
                  .build(),
              variants))
          .build();
    }
  }
}
