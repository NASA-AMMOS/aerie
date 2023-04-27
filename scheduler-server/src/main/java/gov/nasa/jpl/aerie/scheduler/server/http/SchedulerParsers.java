package gov.nasa.jpl.aerie.scheduler.server.http;

import static gov.nasa.jpl.aerie.json.BasicParsers.*;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.scheduler.server.remotes.postgres.PostgresParsers.pgTimestampP;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.scheduler.server.models.HasuraAction;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.models.Timestamp;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleFailure;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;

/**
 * json parsers for data objects used in the scheduler service endpoints
 */
public final class SchedulerParsers {
  private SchedulerParsers() {}

  // TODO: unify common private parsers between services (eg hasura details copied from
  // MerlinParsers)

  public static final JsonParser<SpecificationId> specificationIdP =
      longP.map(SpecificationId::new, SpecificationId::id);

  public static final JsonParser<MissionModelId> missionModelIdP =
      longP.map(MissionModelId::new, MissionModelId::id);

  public static final JsonParser<ScheduleFailure> scheduleFailureP =
      productP
          .field("type", stringP)
          .field("message", stringP)
          .field("data", anyP)
          .optionalField("trace", stringP)
          .field("timestamp", pgTimestampP)
          .map(
              untuple(
                  (type, message, data, trace, timestamp) ->
                      new ScheduleFailure(
                          type, message, data, trace.orElse(""), timestamp.toInstant())),
              failure ->
                  tuple(
                      failure.type(),
                      failure.message(),
                      failure.data(),
                      Optional.ofNullable(failure.trace()),
                      new Timestamp(failure.timestamp())));

  /**
   * parser for hasura session details
   */
  private static final JsonParser<HasuraAction.Session> hasuraActionSessionP =
      productP
          .field("x-hasura-role", stringP)
          .optionalField("x-hasura-user-id", stringP)
          .map(
              untuple((role, userId) -> new HasuraAction.Session(role, userId.orElse(""))),
              session -> tuple(session.hasuraRole(), Optional.ofNullable(session.hasuraUserId())));

  /**
   * creates a parser fragment for a  general hasura action with name / session details that also takes an input arg
   *
   * the parser fragment extracts a tuple of the form (actionName, input, session) which can then be .map()ed in order
   * to establish the object semantics with the matching HasuraAction
   *
   * @param inputP the parser for the input to the hasura action
   * @param <I> the data type of the input to the hasura action
   * @return a parser that accepts hasura action / session details along with specified input type into a tuple
   *     of the form (name,input,session) ready for application of a mapping
   */
  private static <I>
      JsonParser<Pair<Pair<Pair<String, I>, HasuraAction.Session>, String>> hasuraActionP(
          final JsonParser<I> inputP) {
    return productP
        .field("action", productP.field("name", stringP))
        .field("input", inputP)
        .field("session_variables", hasuraActionSessionP)
        .field("request_query", stringP);
  }

  /**
   * parser for a hasura action that accepts a plan id as its sole input, along with normal hasura session details
   */
  public static final JsonParser<HasuraAction<HasuraAction.SpecificationInput>>
      hasuraSpecificationActionP =
          hasuraActionP(productP.field("specificationId", specificationIdP))
              .map(
                  untuple(
                      (name, specificationId, session, requestQuery) ->
                          new HasuraAction<>(
                              name, new HasuraAction.SpecificationInput(specificationId), session)),
                  action ->
                      tuple(action.name(), action.input().specificationId(), action.session(), ""));

  /**
   * parser for a hasura action that accepts a mission model id as its sole input, along with normal hasura session details
   */
  public static final JsonParser<HasuraAction<HasuraAction.MissionModelIdInput>>
      hasuraMissionModelIdActionP =
          hasuraActionP(productP.field("missionModelId", missionModelIdP))
              .map(
                  untuple(
                      (name, missionModelId, session, requestQuery) ->
                          new HasuraAction<>(
                              name, new HasuraAction.MissionModelIdInput(missionModelId), session)),
                  action ->
                      tuple(action.name(), action.input().missionModelId(), action.session(), ""));
}
