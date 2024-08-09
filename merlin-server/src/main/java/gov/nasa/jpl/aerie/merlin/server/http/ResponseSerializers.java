package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.model.ConstraintResult;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.json.JsonParseResult.FailureReason;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.driver.UnfinishedActivity;
import gov.nasa.jpl.aerie.merlin.driver.json.ValueSchemaJsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanDatasetException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.SimulationDatasetMismatchException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelAccessException;
import gov.nasa.jpl.aerie.merlin.server.services.ConstraintsDSLCompilationService;
import gov.nasa.jpl.aerie.merlin.server.services.GetSimulationResultsAction;
import gov.nasa.jpl.aerie.merlin.server.services.LocalMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService.BulkEffectiveArgumentResponse;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService.BulkArgumentValidationResponse;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP;

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

  public static JsonValue serializeValueSchema(final ValueSchema schema) {
    if (schema == null) return JsonValue.NULL;

    return new ValueSchemaJsonParser().unparse(schema);
  }

  public static JsonValue serializeParameters(final List<Parameter> parameters) {
    final var parameterMap = IntStream.range(0, parameters.size()).boxed()
        .collect(Collectors.toMap(i -> parameters.get(i).name(), i -> Pair.of(i, parameters.get(i))));

    return serializeMap(pair -> Json.createObjectBuilder()
            .add("schema", new ValueSchemaJsonParser().unparse(pair.getRight().schema()))
            .add("order", pair.getLeft())
            .build(),
        parameterMap);
  }

  public static JsonValue serializeValueSchemas(final Map<String, ValueSchema> schemas) {
    if (schemas == null) return JsonValue.NULL;

    final var builder = Json.createArrayBuilder();
    schemas.forEach((k, v) -> builder.add(Json.createObjectBuilder()
      .add("name", k)
      .add("schema", serializeValueSchema(v))));
    return builder.build();
  }

  public static JsonValue serializeSample(final Pair<Duration, SerializedValue> element) {
    if (element == null) return JsonValue.NULL;
    return Json
        .createObjectBuilder()
        .add("x", serializeDuration(element.getLeft()))
        .add("y", serializeArgument(element.getRight()))
        .build();
  }

  public static JsonValue serializeString(final String value) {
    if (value == null) return JsonValue.NULL;
    return Json.createValue(value);
  }

  public static JsonValue serializeStringList(final List<String> elements) {
    return serializeIterable(ResponseSerializers::serializeString, elements);
  }

  public static JsonValue serializeArgument(final SerializedValue parameter) {
    if (parameter == null) return JsonValue.NULL;
    return serializedValueP.unparse(parameter);
  }

  public static JsonValue serializeArgumentMap(final Map<String, SerializedValue> fields) {
    return serializeMap(ResponseSerializers::serializeArgument, fields);
  }

  public static JsonValue serializeEffectiveArgumentMap(final Map<String, SerializedValue> fields) {
    return Json.createObjectBuilder()
       .add("success", JsonValue.TRUE)
       .add("arguments", serializeMap(ResponseSerializers::serializeArgument, fields))
       .build();
  }

  public static JsonValue serializeBulkEffectiveArgumentResponseList(final List<BulkEffectiveArgumentResponse> responses) {
    return serializeIterable(ResponseSerializers::serializeBulkEffectiveArgumentResponse, responses);
  }

  public static JsonValue serializeBulkEffectiveArgumentResponse(BulkEffectiveArgumentResponse response) {
    // TODO use pattern matching in switch statement with JDK 21
    if (response instanceof BulkEffectiveArgumentResponse.Success s) {
      return Json.createObjectBuilder()
          .add("typeName",
               s.activity().getTypeName())
          .add("success", JsonValue.TRUE)
          .add("arguments",
               serializeMap(
                   ResponseSerializers::serializeArgument,
                   s.activity().getArguments()))
          .build();
    } else if (response instanceof BulkEffectiveArgumentResponse.TypeFailure f) {
      return Json.createObjectBuilder()
          .add("typeName", f.ex().activityTypeId)
          .add("success", JsonValue.FALSE)
          .add("errors", "No such activity type")
          .build();
    } else if (response instanceof BulkEffectiveArgumentResponse.InstantiationFailure f) {
      return Json.createObjectBuilder(serializeInstantiationException(f.ex()).asJsonObject())
          .add("typeName", f.ex().containerName)
          .build();
    }
    return Json.createObjectBuilder()
        .add("success", JsonValue.FALSE)
        .add("errors", String.format("Internal error: %s", response))
        .build();
  }

  public static JsonValue serializeBulkArgumentValidationResponseList(final List<BulkArgumentValidationResponse> responses) {
    return serializeIterable(ResponseSerializers::serializeBulkArgumentValidationResponse, responses);
  }

  public static JsonValue serializeBulkArgumentValidationResponse(BulkArgumentValidationResponse response) {
    // TODO use pattern matching in switch statement with JDK 21
    if (response instanceof BulkArgumentValidationResponse.Success) {
      return Json.createObjectBuilder()
                 .add("success", JsonValue.TRUE)
                 .build();
    } else if (response instanceof BulkArgumentValidationResponse.Validation v) {
      return Json.createObjectBuilder()
                 .add("success", JsonValue.FALSE)
                 .add("type", "VALIDATION_NOTICES")
                 .add("errors", Json.createObjectBuilder()
                     .add("validationNotices", serializeIterable(ResponseSerializers::serializeValidationNotice, v.notices()))
                     .build())
                 .build();
    } else if (response instanceof BulkArgumentValidationResponse.NoSuchActivityError e) {
      return Json.createObjectBuilder()
                 .add("success", JsonValue.FALSE)
                 .add("type", "NO_SUCH_ACTIVITY_TYPE")
                 .add("errors", Json.createObjectBuilder()
                     .add("noSuchActivityError", serializeNoSuchActivityTypeException(e.ex()))
                     .build())
                 .build();
    } else if (response instanceof BulkArgumentValidationResponse.InstantiationError f) {
      return Json.createObjectBuilder(serializeInstantiationException(f.ex()).asJsonObject())
          .add("type", "INSTANTIATION_ERRORS")
          .build();
    } else if (response instanceof BulkArgumentValidationResponse.NoSuchMissionModelError m) {
      return Json.createObjectBuilder()
                 .add("success", JsonValue.FALSE)
                 .add("type", "NO_SUCH_MISSION_MODEL")
                 .add("errors", Json.createObjectBuilder()
                     .add("noSuchMissionModelError", serializeNoSuchMissionModelException(m.ex()))
                     .build())
                 .build();
    }

    // This should never happen, but we don't have exhaustive pattern matching
    return Json.createObjectBuilder()
               .add("success", JsonValue.FALSE)
               .add("errors", String.format("Internal error: %s", response))
               .build();
  }

  public static JsonValue serializeCreatedDatasetId(final long datasetId) {
    return Json.createObjectBuilder()
        .add("datasetId", datasetId)
        .build();
  }

  public static JsonValue serializeConstraintViolation(final Violation violation) {
    return Json
        .createObjectBuilder()
        .add("windows", serializeIterable(ResponseSerializers::serializeInterval, violation.windows()))
        .add("activityInstanceIds", serializeIterable(Json::createValue, violation.activityInstanceIds()))
        .build();
  }

  public static JsonValue serializeConstraintResult(final ConstraintResult list) {
    return Json
        .createObjectBuilder()
        .add("violations", serializeIterable(ResponseSerializers::serializeConstraintViolation, list.violations))
        .add("gaps", serializeIterable(ResponseSerializers::serializeInterval, list.gaps))
        .add("resourceIds", serializeIterable(Json::createValue, list.resourceIds))
        .build();
  }

  public static JsonValue serializeInterval(final Interval interval) {
    return Json.createObjectBuilder()
               .add("start", interval.start.in(Duration.MICROSECONDS))
               .add("end", interval.end.in(Duration.MICROSECONDS))
               .build();
  }

  private static JsonValue serializeSimulatedActivity(final ActivityInstance activityInstance) {
    return Json
        .createObjectBuilder()
        .add("type", activityInstance.type())
        .add("arguments", serializeArgumentMap(activityInstance.arguments()))
        .add("startTimestamp", serializeTimestamp(activityInstance.start()))
        .add("duration", serializeDuration(activityInstance.duration()))
        .add("parent", serializeNullable(id -> Json.createValue(id.id()), activityInstance.parentId()))
        .add("children", serializeIterable((id -> Json.createValue(id.id())), activityInstance.childIds()))
        .add("computedAttributes", serializeArgument(activityInstance.computedAttributes()))
        .build();
  }

  private static JsonValue serializeSimulatedActivities(final Map<ActivityDirectiveId, ActivityInstance> simulatedActivities) {
    return serializeMap(
        ResponseSerializers::serializeSimulatedActivity,
        simulatedActivities
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(e -> Long.toString(e.getKey().id()), Map.Entry::getValue)));
  }

  private static JsonValue serializeUnfinishedActivity(final UnfinishedActivity simulatedActivity) {
    return Json
        .createObjectBuilder()
        .add("type", simulatedActivity.type())
        .add("arguments", serializeArgumentMap(simulatedActivity.arguments()))
        .add("startTimestamp", serializeTimestamp(simulatedActivity.start()))
        .add("parent", serializeNullable(id -> Json.createValue(id.id()), simulatedActivity.parentId()))
        .add("children", serializeIterable((id -> Json.createValue(id.id())), simulatedActivity.childIds()))
        .build();
  }

  private static JsonValue serializeUnfinishedActivities(final Map<ActivityDirectiveId, UnfinishedActivity> simulatedActivities) {
    return serializeMap(
        ResponseSerializers::serializeUnfinishedActivity,
        simulatedActivities
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(e -> Long.toString(e.getKey().id()), Map.Entry::getValue)));
  }

  private static JsonValue serializeUnconstructableActivityFailure(final MissionModelService.ActivityInstantiationFailure reason) {
    // TODO use pattern-matching switch expression here when available with LTS
    final var builder = Json.createObjectBuilder();
    if (reason instanceof final MissionModelService.ActivityInstantiationFailure.InstantiationFailure r) {
      return builder.add("reason", serializeInstantiationException(r.ex())).build();
    }
    else if (reason instanceof final MissionModelService.ActivityInstantiationFailure.NoSuchActivityType r) {
      return builder.add("reason", serializeNoSuchActivityTypeException(r.ex())).build();
    }
    throw new UnexpectedSubtypeError(MissionModelService.ActivityInstantiationFailure.class, reason);
  }

  public static JsonValue serializeUnconstructableActivityFailures(final Map<ActivityDirectiveId, MissionModelService.ActivityInstantiationFailure> failures) {
    if (failures.isEmpty()) {
      return Json.createObjectBuilder()
        .add("success", JsonValue.TRUE)
        .build();
    }
    return Json.createObjectBuilder()
        .add("success", JsonValue.FALSE)
        .add("errors", serializeMap(
           ResponseSerializers::serializeUnconstructableActivityFailure,
               failures
                   .entrySet()
                   .stream()
                   .collect(
                       Collectors.toMap(e -> Long.toString(e.getKey().id()), Map.Entry::getValue))))
        .build();
  }

  public static JsonValue serializeResourceSamples(final Map<String, List<Pair<Duration, SerializedValue>>> resourceSamples) {
    return Json
        .createObjectBuilder()
        .add("resourceSamples", serializeMap(
            elements -> serializeIterable(ResponseSerializers::serializeSample, elements),
            resourceSamples))
        .build();
  }

  @SuppressWarnings("unchecked")
  public static JsonValue serializeConstraintResults(final Map<Constraint, Fallible<?>> resultMap) {
    var results = resultMap.entrySet().stream().map(entry -> {

      final var constraint = entry.getKey();
      final var fallible = entry.getValue();

      // There should always be a fallible but this is here
      // just in case
      if (fallible.getOptional().isEmpty()) {
        return Json.createObjectBuilder()
                   .add("success", JsonValue.FALSE)
                   .add("constraintId", constraint.id())
                   .add("constraintName", constraint.name())
                   .add("constraintRevision", constraint.revision())
                   .add("errors", Json.createArrayBuilder().add(
                       Json.createObjectBuilder()
                           .add("message", "Internal error processing a constraint")
                           .add("stack", "")
                           .add("location", JsonValue.EMPTY_JSON_OBJECT).build()).build())
                   .add("results", JsonValue.EMPTY_JSON_OBJECT)
                   .build();
      }

      // failure was a compilation error
      if (fallible.isFailure()
          && fallible.getOptional().get() instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error) {
        return serializeConstraintCompileErrors(constraint, (Fallible<ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error>) fallible);
      }

      // failure that are errors exceptions that were captured
      if (fallible.isFailure()) {
        return Json.createObjectBuilder()
                   .add("success", JsonValue.FALSE)
                   .add("constraintId", constraint.id())
                   .add("constraintName", constraint.name())
                   .add("constraintRevision", constraint.revision())
                   .add("errors", Json.createArrayBuilder().add(
                       Json.createObjectBuilder()
                           .add("message", fallible.getMessage())
                           .add("stack", "")
                           .add("location", JsonValue.EMPTY_JSON_OBJECT).build()).build())
                   .add("results", JsonValue.EMPTY_JSON_OBJECT)
                   .build();
      }

      // successful runs
      var constraintResult = (ConstraintResult) fallible.getOptional().get();
      return Json.createObjectBuilder()
                 .add("success", JsonValue.TRUE)
                 .add("constraintId", constraint.id())
                 .add("constraintName", constraint.name())
                 .add("constraintRevision", constraint.revision())
                 .add("errors", JsonValue.EMPTY_JSON_ARRAY)
                 .add("results", serializeConstraintResult(constraintResult))
                 .build();

    }).toList();

    final var resultsArrayBuilder = Json.createArrayBuilder();
    results.forEach(resultsArrayBuilder::add);

    return resultsArrayBuilder.build();
  }

  public static JsonValue serializeSimulationResultsResponse(final GetSimulationResultsAction.Response response) {
      return switch (response) {
          case GetSimulationResultsAction.Response.Pending r -> Json
                  .createObjectBuilder()
                  .add("status", "pending")
                  .add("simulationDatasetId", r.simulationDatasetId())
                  .build();
          case GetSimulationResultsAction.Response.Incomplete r -> Json
                  .createObjectBuilder()
                  .add("status", "incomplete")
                  .add("simulationDatasetId", r.simulationDatasetId())
                  .build();
          case GetSimulationResultsAction.Response.Failed r -> Json
                  .createObjectBuilder()
                  .add("status", "failed")
                  .add("simulationDatasetId", r.simulationDatasetId())
                  .add("reason", MerlinParsers.simulationFailureP.unparse(r.reason()))
                  .build();
          case GetSimulationResultsAction.Response.Complete r -> Json
                  .createObjectBuilder()
                  .add("status", "complete")
                  .add("simulationDatasetId", r.simulationDatasetId())
                  .build();
          case null -> throw new IllegalArgumentException("simulation results action response was null");
      };
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

  public static JsonValue serializeFailures(final List<String> failures) {
    if (!failures.isEmpty()) {
      return Json.createObjectBuilder()
                 .add("success", JsonValue.FALSE)
                 .add("errors", Json.createArrayBuilder(failures))
                 .build();
    } else {
      return Json.createObjectBuilder()
                 .add("success", JsonValue.TRUE)
                 .build();
    }
  }

  public static JsonValue serializeValidationNotices(final List<ValidationNotice> notices) {
    if (!notices.isEmpty()) {
      return Json.createObjectBuilder()
          .add("success", JsonValue.FALSE)
          .add("errors", serializeIterable(ResponseSerializers::serializeValidationNotice, notices))
          .build();
    } else {
      return Json.createObjectBuilder()
          .add("success", JsonValue.TRUE)
          .build();
}
  }

  private static JsonValue serializeValidationNotice(final ValidationNotice notice) {
    return Json.createObjectBuilder()
        .add("subjects", serializeStringList(notice.subjects()))
        .add("message", notice.message())
        .build();
  }

  public static JsonValue serializeInstantiationException(final InstantiationException ex) {
    return Json.createObjectBuilder()
        .add("success", JsonValue.FALSE)
        .add("errors", Json.createObjectBuilder()
            .add("extraneousArguments", serializeStringList(ex.extraneousArguments.stream().map(a -> a.parameterName()).toList()))
            .add("unconstructableArguments", serializeIterable(ResponseSerializers::serializeUnconstructableArgument, ex.unconstructableArguments))
            .add("missingArguments", serializeStringList(ex.missingArguments.stream().map(a -> a.parameterName()).toList()))
            .build())
        .add("arguments", serializeMap(ResponseSerializers::serializeArgument, ex.validArguments.stream().collect(Collectors.toMap(
             InstantiationException.ValidArgument::parameterName,
             InstantiationException.ValidArgument::serializedValue))))
        .build();
  }

  private static JsonValue serializeUnconstructableArgument(
      final InstantiationException.UnconstructableArgument argument)
  {
    return Json.createObjectBuilder()
       .add("name", argument.parameterName())
       .add("failure", argument.failure())
       .build();
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

  public static JsonValue serializeConstraintCompileErrors(final Constraint constraint, final Fallible<ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error> ex) {

    final var userCodeError = ex
        .getOptional()
        .orElse(new ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error(new ArrayList<>()))
        .errors()
        .stream()
        .map(UserCodeError -> Json.createObjectBuilder()
                                  .add("stack", UserCodeError.stack())
                                  .add("message", ex.getMessage() + UserCodeError.message())
                                  .add("location", Json.createObjectBuilder()
                                                       .add("line", UserCodeError.location().line())
                                                       .add("column", UserCodeError.location().column()).build())
                                  .build())
        .toList();

    final var userCodeErrorArrayBuilder = Json.createArrayBuilder();
    userCodeError.forEach(userCodeErrorArrayBuilder::add);

    return Json.createObjectBuilder()
               .add("success", JsonValue.FALSE)
               .add("constraintId", constraint.id())
               .add("constraintName", constraint.name())
               .add("constraintRevision", constraint.revision())
               .add("errors", userCodeErrorArrayBuilder.build())
               .add("results", Json.createObjectBuilder().build())
               .build();
  }

  public static JsonValue serializeInvalidEntityException(final InvalidEntityException ex) {
    return Json.createObjectBuilder()
               .add("kind", "invalid-entity")
               .add("failures", serializeIterable(ResponseSerializers::serializeFailureReason, ex.failures))
               .build();
  }

  public static JsonValue serializeMissionModelLoadException(
      final LocalMissionModelService.MissionModelLoadException ex)
  {
    // TODO: Improve diagnostic information?
    return Json.createObjectBuilder()
               .add("message", ex.getMessage())
               .add("type", "Mission Model Load Failure")
               .build();
  }

  public static JsonValue serializeMissionModelAccessException(final MissionModelAccessException ex) {
    // TODO: Improve diagnostic information?
    return Json.createObjectBuilder()
               .add("message", ex.getMessage())
               .build();
  }

  public static JsonValue serializeFailureReason(final FailureReason failure) {
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

  public static JsonValue serializeNoSuchPlanException(final NoSuchPlanException ex) {
    return Json.createObjectBuilder()
        .add("message", "no such plan")
        .add("plan_id", ex.id.id())
        .build();
  }

  public static JsonValue serializeNoSuchPlanDatasetException(final NoSuchPlanDatasetException ex) {
    return Json.createObjectBuilder()
               .add("message", "no such plan dataset")
               .add("plan_id", ex.id.id())
               .build();
  }

  public static JsonValue serializeNoSuchMissionModelException(final MissionModelService.NoSuchMissionModelException ex) {
    return Json.createObjectBuilder()
        .add("message", "no such mission model")
        .add("mission_model_id", ex.missionModelId.id())
        .build();
  }

  public static JsonValue serializeNoSuchActivityTypeException(final MissionModelService.NoSuchActivityTypeException ex) {
    return Json.createObjectBuilder()
        .add("message", "no such activity type")
        .add("activity_type", ex.activityTypeId)
        .build();
  }

  public static JsonValue serializeInputMismatchException(final InputMismatchException ex) {
    return Json.createObjectBuilder()
               .add("message", "input mismatch exception")
               .add("cause", ex.getMessage())
               .build();
  }

  public static JsonValue serializeSimulationDatasetMismatchException(final SimulationDatasetMismatchException ex){
     return Json.createObjectBuilder()
               .add("message", "simulation dataset mismatch exception")
               .add("cause", ex.getMessage())
               .build();
  }
}
