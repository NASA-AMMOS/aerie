package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.services.AdaptationService;
import gov.nasa.jpl.aerie.merlin.server.services.GetSimulationResultsAction;
import gov.nasa.jpl.aerie.merlin.server.services.PlanService;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;
import io.javalin.http.Context;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import java.io.StringReader;
import java.util.List;

import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.hasuraAdaptationActionP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.hasuraMissionModelEventTriggerP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.hasuraPlanActionP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.hasuraValidateActivityActionP;
import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

/**
 * Lift native Java agents into an HTTP-oriented service.
 *
 * The role of a {@code MerlinBindings} object is to faithfully translate between the request/response protocol
 * of HTTP and the call/return/throw protocol of a Java method. Put differently, {@code MerlinBindings} <i>lifts</i>
 * an object with native Java endpoints (methods) into an HTTP service with HTTP-oriented endpoints. This entails
 * translating HTTP request bodies into native Java domain objects, and translating native Java domain objects
 * (including thrown exceptions) into HTTP response bodies.
 *
 * The object to be lifted must implement the {@link PlanService} interface. Formally, it is
 * this interface that the {@code MerlinBindings} class lifts into the domain of HTTP; an object implementing
 * this interface defines the action to take for each HTTP request in an HTTP-independent way.
 */
public final class MerlinBindings implements Plugin {
  private final PlanService planService;
  private final AdaptationService adaptationService;
  private final GetSimulationResultsAction simulationAction;

  public MerlinBindings(
      final PlanService planService,
      final AdaptationService adaptationService,
      final GetSimulationResultsAction simulationAction)
  {
    this.planService = planService;
    this.adaptationService = adaptationService;
    this.simulationAction = simulationAction;
  }

  @Override
  public void apply(final Javalin javalin) {
    javalin.routes(() -> {
      before(ctx -> ctx.contentType("application/json"));

      path("resourceTypes", () -> {
        post(this::getResourceTypes);
      });
      path("getSimulationResults", () -> {
        post(this::getSimulationResults);
      });
      path("refreshModelParameters", () -> {
        post(this::postRefreshModelParameters);
      });
      path("refreshActivityTypes", () -> {
        post(this::postRefreshActivityTypes);
      });
      path("validateActivityArguments", () -> {
        post(this::validateActivityArguments);
      });
    });

    // This exception is expected when the request body entity is not a legal JsonValue.
    javalin.exception(JsonParsingException.class, (ex, ctx) -> ctx
        .status(400)
        .result(ResponseSerializers.serializeJsonParsingException(ex).toString())
        .contentType("application/json"));
  }

  private void postRefreshModelParameters(final Context ctx) {
    try {
      final var adaptationId = parseJson(ctx.body(), hasuraMissionModelEventTriggerP).adaptationId();
      this.adaptationService.refreshModelParameters(adaptationId);
      ctx.status(200);
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    }
  }

  private void postRefreshActivityTypes(final Context ctx) {
    try {
      final var adaptationId = parseJson(ctx.body(), hasuraMissionModelEventTriggerP).adaptationId();
      this.adaptationService.refreshActivityTypes(adaptationId);
      ctx.status(200);
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    }
  }

  private void getResourceTypes(final Context ctx) {
    try {
      final var adaptationId = parseJson(ctx.body(), hasuraAdaptationActionP).input().adaptationId();

      final var schemaMap = this.adaptationService.getStatesSchemas(adaptationId);

      ctx.result(ResponseSerializers.serializeValueSchemas(schemaMap).toString());
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    }
  }

  private void getSimulationResults(final Context ctx) {
    try {
      final var body = parseJson(ctx.body(), hasuraPlanActionP);
      final var planId = body.input().planId();

      final var response = this.simulationAction.run(planId);
      ctx.result(ResponseSerializers.serializeSimulationResultsResponse(response).toString());

    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch(final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    }
  }

  private void validateActivityArguments(final Context ctx) {
    try {
      final var input = parseJson(ctx.body(), hasuraValidateActivityActionP).input();

      final var missionModelId = input.missionModelId();
      final var activityTypeName = input.activityTypeName();
      final var activityArguments = input.arguments();

      final var serializedActivity = new SerializedActivity(activityTypeName, activityArguments);

      final var failures = this.adaptationService.validateActivityParameters(missionModelId, serializedActivity);

      ctx.result(ResponseSerializers.serializeFailures(failures).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    }
  }

  private <T> T parseJson(final String subject, final JsonParser<T> parser)
  throws InvalidJsonException, InvalidEntityException
  {
    try {
      final var requestJson = Json.createReader(new StringReader(subject)).readValue();
      final var result = parser.parse(requestJson);
      return result.getSuccessOrThrow($ -> new InvalidEntityException(List.of($)));
    } catch (JsonParsingException e) {
      throw new InvalidJsonException(e);
    }
  }
}
