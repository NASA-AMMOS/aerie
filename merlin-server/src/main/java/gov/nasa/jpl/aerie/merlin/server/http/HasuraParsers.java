package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraAction;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraActivityDirectiveEvent;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraMissionModelEvent;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.planIdP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.timestampP;
import static gov.nasa.jpl.aerie.merlin.server.http.ProfileParsers.profileSetP;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.pgTimestampP;

public abstract class HasuraParsers {
  private HasuraParsers() {}

  private static final JsonParser<HasuraAction.Session> hasuraActionSessionP
      = productP
      .field("x-hasura-role", stringP)
      .optionalField("x-hasura-user-id", stringP)
      .map(
          untuple((role, userId) -> new HasuraAction.Session(role, userId.orElse(""))),
          $ -> tuple($.hasuraRole(), Optional.ofNullable($.hasuraUserId())));

  private static <I> JsonParser<Pair<Pair<Pair<String, I>, HasuraAction.Session>, String>> hasuraActionP(final JsonParser<I> inputP) {
    return productP
        .field("action", productP.field("name", stringP))
        .field("input", inputP)
        .field("session_variables", hasuraActionSessionP)
        .field("request_query", stringP);
  }

  public static final JsonParser<HasuraAction<HasuraAction.MissionModelInput>> hasuraMissionModelActionP
      = hasuraActionP(productP.field("missionModelId", stringP))
      .map(
          untuple((name, missionModelId, session, requestQuery) -> new HasuraAction<>(name, new HasuraAction.MissionModelInput(missionModelId), session)),
          $ -> tuple($.name(), $.input().missionModelId(), $.session(), ""));

  public static final JsonParser<HasuraAction<HasuraAction.PlanInput>> hasuraPlanActionP
      = hasuraActionP(productP.field("planId", planIdP))
      .map(
          untuple((name, planId, session, requestQuery) -> new HasuraAction<>(name, new HasuraAction.PlanInput(planId), session)),
          $ -> tuple($.name(), $.input().planId(), $.session(), ""));

  public static final JsonParser<HasuraMissionModelEvent> hasuraMissionModelEventTriggerP
      = productP
      .field("event", productP
          .field("data", productP
              .field("new", productP
                  .field("id", longP)
                  .rest())
              .rest())
          .rest())
      .rest()
      .map(
          untuple(missionModelId -> new HasuraMissionModelEvent(String.valueOf(missionModelId))),
          $ -> tuple(Long.parseLong($.missionModelId())));

  public static final JsonParser<HasuraActivityDirectiveEvent> hasuraActivityDirectiveEventTriggerP
      = productP
      .field("event", productP
          .field("data", productP
              .field("new", productP
                  .field("plan_id", longP)
                  .field("id", longP)
                  .field("type", stringP)
                  .field("arguments", mapP(serializedValueP))
                  .field("last_modified_arguments_at", pgTimestampP)
                  .rest())
              .rest())
          .rest())
      .rest()
      .map(
          untuple((planId, activityDirectiveId, type, arguments, argumentsModifiedTime) ->
              new HasuraActivityDirectiveEvent(new PlanId(planId), new ActivityDirectiveId(activityDirectiveId), type, arguments, argumentsModifiedTime)),
          $ -> tuple($.planId().id(), $.activityDirectiveId().id(), $.activityTypeName(), $.arguments(), $.argumentsModifiedTime()));

  private static final JsonParser<HasuraAction.MissionModelArgumentsInput> hasuraMissionModelArgumentsInputP
      = productP
      .field("missionModelId", stringP)
      .field("modelArguments", mapP(serializedValueP))
      .map(
          untuple(HasuraAction.MissionModelArgumentsInput::new),
          $ -> tuple($.missionModelId(), $.arguments()));

  public static final JsonParser<HasuraAction<HasuraAction.MissionModelArgumentsInput>> hasuraMissionModelArgumentsActionP
      = hasuraActionP(hasuraMissionModelArgumentsInputP)
      .map(
          untuple((name, input, session, requestQuery) -> new HasuraAction<>(name, input, session)),
          $ -> tuple($.name(), $.input(), $.session(), ""));

  private static final JsonParser<HasuraAction.ActivityInput> hasuraActivityInputP
      = productP
      .field("missionModelId", stringP)
      .field("activityTypeName", stringP)
      .field("activityArguments", mapP(serializedValueP))
      .map(
          untuple(HasuraAction.ActivityInput::new),
          $ -> tuple($.missionModelId(), $.activityTypeName(), $.arguments()));

  public static final JsonParser<HasuraAction<HasuraAction.ActivityInput>> hasuraActivityActionP
      = hasuraActionP(hasuraActivityInputP)
      .map(
          untuple((name, input, session, requestQuery) -> new HasuraAction<>(name, input, session)),
          $ -> tuple($.name(), $.input(), $.session(), ""));

  public static final JsonParser<HasuraAction.UploadExternalDatasetInput> hasuraUploadExternalDatasetActionP
      = productP
      .field("planId", planIdP)
      .field("datasetStart", timestampP)
      .field("profileSet", profileSetP)
      .map(
          untuple(HasuraAction.UploadExternalDatasetInput::new),
          $ -> tuple($.planId(), $.datasetStart(), $.profileSet()));

  public static final JsonParser<HasuraAction<HasuraAction.UploadExternalDatasetInput>> hasuraExternalDatasetActionP
      = hasuraActionP(hasuraUploadExternalDatasetActionP)
      .map(
          untuple((name, input, session, requestQuery) -> new HasuraAction<>(name, input, session)),
          $ -> tuple($.name(), $.input(), $.session(), ""));
}
