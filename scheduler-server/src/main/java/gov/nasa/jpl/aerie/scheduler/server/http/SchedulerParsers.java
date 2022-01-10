package gov.nasa.jpl.aerie.scheduler.server.http;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraAction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.planIdP;

/**
 * json parsers for data objects used in the scheduler service endpoints
 */
public class SchedulerParsers {
  private SchedulerParsers() {}

  //TODO: unify common private parsers between services (eg hasura details copied from MerlinParsers)

  /**
   * parser for hasura session details
   */
  private static final JsonParser<HasuraAction.Session> hasuraActionSessionP = productP
      .field("x-hasura-role", stringP)
      .optionalField("x-hasura-user-id", stringP)
      .map(Iso.of(
          untuple((role, userId) -> new HasuraAction.Session(role, userId.orElse(""))),
          session -> tuple(session.hasuraRole(), Optional.ofNullable(session.hasuraUserId()))));

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
  private static <I> JsonParser<Pair<Pair<String, I>, HasuraAction.Session>> hasuraActionP(final JsonParser<I> inputP) {
    return productP
        .field("action", productP.field("name", stringP))
        .field("input", inputP)
        .field("session_variables", hasuraActionSessionP);
  }

  /**
   * parser for a hasura action that accepts a plan id as its sole input, along with normal hasura session details
   */
  public static final JsonParser<HasuraAction<HasuraAction.PlanInput>> hasuraPlanActionP
      = hasuraActionP(productP.field("planId", planIdP))
      .map(Iso.of(
          untuple((name, planId, session) -> new HasuraAction<>(name, new HasuraAction.PlanInput(planId), session)),
          action -> tuple(action.name(), action.input().planId(), action.session())));


}
