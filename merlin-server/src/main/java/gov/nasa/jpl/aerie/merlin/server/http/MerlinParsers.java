package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Uncurry;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraAction;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraMissionModelEvent;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.services.CreateSimulationMessage;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.server.http.ProfileParsers.profileSetP;

public abstract class MerlinParsers {
  private MerlinParsers() {}

  public static final JsonParser<Timestamp> timestampP = new JsonParser<>() {
    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      return Json
          .createObjectBuilder(stringP.getSchema())
          .add("format", "date-time")
          .build();
    }

    @Override
    public JsonParseResult<Timestamp> parse(final JsonValue json) {
      final var result = stringP.parse(json);
      if (result instanceof JsonParseResult.Success<String> s) {
        try {
          return JsonParseResult.success(Timestamp.fromString(s.result()));
        } catch (DateTimeParseException e) {
          return JsonParseResult.failure("invalid timestamp format");
        }
      } else if (result instanceof JsonParseResult.Failure<?> f) {
        return f.cast();
      } else {
        throw new UnexpectedSubtypeError(JsonParseResult.class, result);
      }
    }

    @Override
    public JsonValue unparse(final Timestamp value) {
      return stringP.unparse(value.toString());
    }
  };

  public static final JsonParser<Duration> durationP
      = longP
      . map(Iso.of(
          microseconds -> Duration.of(microseconds, Duration.MICROSECONDS),
          duration -> duration.in(Duration.MICROSECONDS)));

  private static final JsonParser<HasuraAction.Session> hasuraActionSessionP
      = productP
      . field("x-hasura-role", stringP)
      . optionalField("x-hasura-user-id", stringP)
      . map(Iso.of(
          untuple((role, userId) -> new HasuraAction.Session(role, userId.orElse(""))),
          $ -> tuple($.hasuraRole(), Optional.ofNullable($.hasuraUserId()))));

  private static <S> JsonParser<Pair<Pair<String, S>, HasuraAction.Session>> hasuraActionP(final JsonParser<S> inputP) {
    return productP
        .  field("action", productP.field("name", stringP))
        .  field("input", inputP)
        .  field("session_variables", hasuraActionSessionP);
  }

  public static final JsonParser<HasuraAction<HasuraAction.MissionModelInput>> hasuraMissionModelActionP
      = hasuraActionP(productP.field("missionModelId", stringP))
      . map(Iso.of(
          untuple((name, missionModelId, session) -> new HasuraAction<>(name, new HasuraAction.MissionModelInput(missionModelId), session)),
          $ -> tuple($.name(), $.input().missionModelId(), $.session())));

  public static final JsonParser<HasuraAction<HasuraAction.PlanInput>> hasuraPlanActionP
      = hasuraActionP(productP.field("planId", stringP))
      . map(Iso.of(
          untuple((name, planId, session) -> new HasuraAction<>(name, new HasuraAction.PlanInput(planId), session)),
          $ -> tuple($.name(), $.input().planId(), $.session())));

  public static final JsonParser<HasuraMissionModelEvent> hasuraMissionModelEventTriggerP
      = productP
      . field("event", productP
          .field("data", productP
              .field("new", productP
                  .field("id", longP)
                  .rest()
                  .map(Iso.of(untuple(id -> id), Uncurry::tuple)))
              .rest()
              .map(Iso.of(untuple(newDataId -> newDataId), Uncurry::tuple)))
          .rest()
          .map(Iso.of(untuple(dataId -> dataId), Uncurry::tuple)))
      . rest()
      . map(Iso.of(
          untuple(missionModelId -> new HasuraMissionModelEvent(String.valueOf(missionModelId))),
          $ -> tuple(Long.parseLong($.missionModelId()))));

  private static final JsonParser<HasuraAction.ActivityInput> hasuraActivityInputP
      = productP
      . field("missionModelId", stringP)
      . field("activityTypeName", stringP)
      . field("activityArguments", mapP(serializedValueP))
      . map(Iso.of(
          untuple(HasuraAction.ActivityInput::new),
          $ -> tuple($.missionModelId(), $.activityTypeName(), $.arguments())));

  public static final JsonParser<HasuraAction<HasuraAction.ActivityInput>> hasuraActivityActionP
      = hasuraActionP(hasuraActivityInputP)
      . map(Iso.of(
          untuple(HasuraAction::new),
          $ -> tuple($.name(), $.input(), $.session())));

  public static final JsonParser<HasuraAction.UploadExternalDatasetInput> hasuraUploadExternalDatasetActionP
      = productP
      . field("planId", stringP)
      . field("datasetStart", timestampP)
      . field("profileSet", profileSetP)
      . map(Iso.of(
          untuple(HasuraAction.UploadExternalDatasetInput::new),
          $ -> tuple($.planId(), $.datasetStart(), $.profileSet())
      ));

  public static final JsonParser<HasuraAction<HasuraAction.UploadExternalDatasetInput>> hasuraExternalDatasetActionP
      = hasuraActionP(hasuraUploadExternalDatasetActionP)
      . map(Iso.of(
          untuple(HasuraAction::new),
          $ -> tuple($.name(), $.input(), $.session())));
}
