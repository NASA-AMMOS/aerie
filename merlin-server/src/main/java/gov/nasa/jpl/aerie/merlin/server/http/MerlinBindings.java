package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.server.services.AdaptationService;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationFacade;
import gov.nasa.jpl.aerie.merlin.server.models.NewAdaptation;
import gov.nasa.jpl.aerie.merlin.server.services.PlanService;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.ValidationException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.CreatedEntity;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.activityInstanceP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.activityInstancePatchP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.createSimulationMessageP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.newPlanP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.planPatchP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.serializedParameterP;
import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.head;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

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

  public MerlinBindings(
      final PlanService planService,
      final AdaptationService adaptationService)
  {
    this.planService = planService;
    this.adaptationService = adaptationService;
  }

  @Override
  public void apply(final Javalin javalin) {
    javalin.routes(() -> {
      before(ctx -> ctx.contentType("application/json"));

      path("plans", () -> {
        get(this::getPlans);
        post(this::postPlan);
        path(":planId", () -> {
          get(this::getPlan);
          put(this::putPlan);
          patch(this::patchPlan);
          delete(this::deletePlan);
          path("activity_instances", () -> {
            get(this::getActivityInstances);
            post(this::postActivityInstances);
            path(":activityInstanceId", () -> {
              get(this::getActivityInstance);
              put(this::putActivityInstance);
              patch(this::patchActivityInstance);
              delete(this::deleteActivityInstance);
            });
          });
          path("results", () -> {
            get(this::getSimulationResults);
          });
        });
      });

      path("adaptations", () -> {
        get(this::getAdaptations);
        post(this::postAdaptation);
        path(":adaptationId", () -> {
          head(this::doesAdaptationExist);
          get(this::getAdaptation);
          delete(this::deleteAdaptation);
          path("activities", () -> {
            get(this::getActivityTypes);
            path(":activityTypeId", () -> {
              get(this::getActivityType);
              path("validate", () -> {
                post(this::validateActivityParameters);
              });
            });
          });
          path("constraints", () -> {
            get(this::getConstraints);
            post(this::postModelConstraints);
          });
          path("stateSchemas", () -> {
            get(this::getStateSchemas);
          });
        });
      });

      path("simulations", () -> {
        post(this::runSimulation);
      });
    });

    // This exception is expected when the request body entity is not a legal JsonValue.
    javalin.exception(JsonParsingException.class, (ex, ctx) -> ctx
        .status(400)
        .result(ResponseSerializers.serializeJsonParsingException(ex).toString())
        .contentType("application/json"));
  }

  private void getSimulationResults(final Context ctx) {
    try {
      final var planId = ctx.pathParam("planId");

      final var results = this.planService.getSimulationResultsForPlan(planId);

      ctx.result(ResponseSerializers.serializeSimulationResults(results).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    }
  }

  private void getPlans(final Context ctx) {
    final Map<String, Plan> plans = this.planService
        .getPlans()
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    ctx.result(ResponseSerializers.serializePlanMap(plans).toString());
  }

  private void getPlan(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");

      final Plan plan = this.planService.getPlanById(planId);

      ctx.result(ResponseSerializers.serializePlan(plan).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    }
  }

  private void postPlan(final Context ctx) {
    try {
      final NewPlan plan = parseJson(ctx.body(), newPlanP);

      final String planId = this.planService.addPlan(plan);

      ctx
          .status(201)
          .header("Location", "/plans/" + planId)
          .result(ResponseSerializers.serializeCreatedEntity(new CreatedEntity(planId)).toString());
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final ValidationException ex) {
      ctx.status(422).result(ResponseSerializers.serializeValidationException(ex).toString());
    }
  }

  private void putPlan(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");
      final NewPlan plan = parseJson(ctx.body(), newPlanP);

      this.planService.replacePlan(planId, plan);
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    } catch (final ValidationException ex) {
      ctx.status(422).result(ResponseSerializers.serializeValidationException(ex).toString());
    }
  }

  private void patchPlan(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");
      final Plan patch = parseJson(ctx.body(), planPatchP);

      this.planService.updatePlan(planId, patch);
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    } catch (final NoSuchActivityInstanceException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchActivityInstanceException(ex).toString());
    } catch (final ValidationException ex) {
      ctx.status(422).result(ResponseSerializers.serializeValidationException(ex).toString());
    }
  }

  private void deletePlan(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");

      this.planService.removePlan(planId);
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    }
  }

  private void getActivityInstances(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");

      final Plan plan = this.planService.getPlanById(planId);

      ctx.result(ResponseSerializers.serializeActivityInstanceMap(plan.activityInstances).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    }
  }

  private void postActivityInstances(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");
      final List<ActivityInstance> activityInstances = parseJson(ctx.body(), listP(activityInstanceP));

      final List<String> activityInstanceIds = this.planService.addActivityInstancesToPlan(planId, activityInstances);

      ctx.result(ResponseSerializers.serializeStringList(activityInstanceIds).toString());
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    } catch (final ValidationException ex) {
      ctx.status(422).result(ResponseSerializers.serializeValidationException(ex).toString());
    }
  }

  private void getActivityInstance(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");
      final String activityInstanceId = ctx.pathParam("activityInstanceId");

      final ActivityInstance activityInstance = this.planService.getActivityInstanceById(planId, activityInstanceId);

      ctx.result(ResponseSerializers.serializeActivityInstance(activityInstance).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    } catch (final NoSuchActivityInstanceException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchActivityInstanceException(ex).toString());
    }
  }

  private void putActivityInstance(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");
      final String activityInstanceId = ctx.pathParam("activityInstanceId");
      final ActivityInstance activityInstance = parseJson(ctx.body(), activityInstanceP);

      this.planService.replaceActivityInstance(planId, activityInstanceId, activityInstance);
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    } catch (final NoSuchActivityInstanceException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchActivityInstanceException(ex).toString());
    } catch (final ValidationException ex) {
      ctx.status(422).result(ResponseSerializers.serializeValidationException(ex).toString());
    }
  }

  private void patchActivityInstance(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");
      final String activityInstanceId = ctx.pathParam("activityInstanceId");
      final ActivityInstance activityInstance = parseJson(ctx.body(), activityInstancePatchP);

      this.planService.updateActivityInstance(planId, activityInstanceId, activityInstance);
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    } catch (final NoSuchActivityInstanceException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchActivityInstanceException(ex).toString());
    } catch (final ValidationException ex) {
      ctx.status(422).result(ResponseSerializers.serializeValidationException(ex).toString());
    }
  }

  private void deleteActivityInstance(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");
      final String activityInstanceId = ctx.pathParam("activityInstanceId");

      this.planService.removeActivityInstanceById(planId, activityInstanceId);
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    } catch (final NoSuchActivityInstanceException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchActivityInstanceException(ex).toString());
    }
  }

  private void getAdaptations(final Context ctx) {
    final var adaptations = this.adaptationService.getAdaptations();

    ctx.result(ResponseSerializers.serializeAdaptations(adaptations).toString());
  }

  private void postAdaptation(final Context ctx) {
    try {
      final var newAdaptation = readNewAdaptation(ctx);

      final var adaptationId = this.adaptationService.addAdaptation(newAdaptation);

      ctx.status(201)
         .header("Location", "/adaptations/" + adaptationId)
         .result(ResponseSerializers.serializedCreatedId(adaptationId).toString());
    } catch (final AdaptationService.AdaptationRejectedException ex) {
      ctx.status(400).result(ResponseSerializers.serializeAdaptationRejectedException(ex).toString());
    } catch (final NewAdaptationValidationException ex) {
      ctx.status(400).result(ResponseSerializers.serializeValidationException(ex).toString());
    }
  }

  private void doesAdaptationExist(final Context ctx) {
    try {
      final var adaptationId = ctx.pathParam("adaptationId");

      this.adaptationService.getAdaptationById(adaptationId);

      ctx.status(200);
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    }
  }

  private void getAdaptation(final Context ctx) {
    try {
      final var adaptationId = ctx.pathParam("adaptationId");

      final var adaptationJar = this.adaptationService.getAdaptationById(adaptationId);

      ctx.result(ResponseSerializers.serializeAdaptation(adaptationJar).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    }
  }

  private void deleteAdaptation(final Context ctx) {
    try {
      final var adaptationId = ctx.pathParam("adaptationId");

      this.adaptationService.removeAdaptation(adaptationId);
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    }
  }

  private void getConstraints(final Context ctx) {
    try {
      final var adaptationId = ctx.pathParam("adaptationId");

      final var constraints = this.adaptationService.getConstraints(adaptationId);

      ctx.result(ResponseSerializers.serializeConstraints(constraints).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    }
  }

  private void postModelConstraints(final Context ctx) {
    try {
      final var adaptationId = ctx.pathParam("adaptationId");
      final var constraints = parseJson(ctx.body(), mapP(stringP));

      this.adaptationService.replaceConstraints(adaptationId, constraints);

      ctx.status(200);
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    }
  }

  private void getStateSchemas(final Context ctx) {
    try {
      final var adaptationId = ctx.pathParam("adaptationId");

      final var schemaMap = this.adaptationService.getStatesSchemas(adaptationId);

      ctx.result(ResponseSerializers.serializeValueSchemas(schemaMap).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    }
  }

  private void getActivityTypes(final Context ctx) {
    try {
      final var adaptationId = ctx.pathParam("adaptationId");

      final var activityTypes = this.adaptationService.getActivityTypes(adaptationId);

      ctx.result(ResponseSerializers.serializeActivityTypes(activityTypes).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    }
  }

  private void getActivityType(final Context ctx) {
    try {
      final var adaptationId = ctx.pathParam("adaptationId");
      final var activityTypeId = ctx.pathParam("activityTypeId");

      final var activityType = this.adaptationService.getActivityType(adaptationId, activityTypeId);

      ctx.result(ResponseSerializers.serializeActivityType(activityType).toString());
    } catch (final AdaptationService.NoSuchAdaptationException
        | AdaptationService.NoSuchActivityTypeException ex) {
      ctx.status(404);
    }
  }

  private void validateActivityParameters(final Context ctx) {
    try {
      final var adaptationId = ctx.pathParam("adaptationId");
      final var activityTypeId = ctx.pathParam("activityTypeId");

      final var activityParameters = parseJson(ctx.body(), mapP(serializedParameterP));
      final var serializedActivity = new SerializedActivity(activityTypeId, activityParameters);

      final var failures = this.adaptationService.validateActivityParameters(adaptationId, serializedActivity);

      ctx.result(ResponseSerializers.serializeFailureList(failures).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      ctx.status(404);
    } catch (final InvalidJsonException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    }
  }

  private void runSimulation(final Context ctx) {
    try {
      final var message = parseJson(ctx.body(), createSimulationMessageP);

      final var results = this.adaptationService.runSimulation(message);

      ctx.result(ResponseSerializers.serializeSimulationResults(results).toString());
    } catch (final JsonParsingException ex) {
      // Request entity is not valid JSON.
      // TODO: report this failure with a better response body
      ctx.status(400).result(Json.createObjectBuilder().add("kind", "invalid-json").build().toString());
    } catch (final InvalidJsonException ex) {
      // Request body is invalid json
      ctx.status(400).result(ResponseSerializers.serializeInvalidJsonException(ex).toString());
    } catch (final InvalidEntityException ex) {
      // Request entity does not have the expected shape
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      // The requested adaptation does not exist.
      ctx.status(404);
    } catch (final SimulationDriver.TaskSpecInstantiationException e) {
      // The schedule contained an invalid activity instance
      ctx.status(400).result(ResponseSerializers.serializeTaskSpecInstantiationException(e).toString());
    } catch (final AdaptationFacade.NoSuchActivityTypeException e) {
      // The adaptation could not instantiate the provided activities.
      // TODO: report these failures with a better response body
      ctx.status(400).result(Json.createObjectBuilder().add("kind", "invalid-activities").build().toString());
    }
  }

  private NewAdaptation readNewAdaptation(final Context ctx) throws NewAdaptationValidationException {
    final var validationErrors = new ArrayList<String>();

    String name = null;
    String version = null;
    String mission = null;
    String owner = null;
    UploadedFile uploadedFile = null;
    {
      for (final var formParam : ctx.formParamMap().entrySet()) {
        final String key = formParam.getKey();
        final List<String> values = formParam.getValue();

        switch (key) {
          case "name":
            if (values.isEmpty()) {
              validationErrors.add("Zero values provided for key `" + key + "`; expected one.");
            } else if (values.size() > 1) {
              validationErrors.add("Multiple values provided for key `"
                                   + key
                                   + "`; expected one.");
            } else {
              name = values.get(0);
            }
            break;
          case "version":
            if (values.isEmpty()) {
              validationErrors.add("Zero values provided for key `" + key + "`; expected one.");
            } else if (values.size() > 1) {
              validationErrors.add("Multiple values provided for key `"
                                   + key
                                   + "`; expected one.");
            } else {
              version = values.get(0);
            }
            break;
          case "mission":
            if (values.isEmpty()) {
              validationErrors.add("Zero values provided for key `" + key + "`; expected one.");
            } else if (values.size() > 1) {
              validationErrors.add("Multiple values provided for key `"
                                   + key
                                   + "`; expected one.");
            } else {
              mission = values.get(0);
            }
            break;
          case "owner":
            if (values.isEmpty()) {
              validationErrors.add("Zero values provided for key `" + key + "`; expected one.");
            } else if (values.size() > 1) {
              validationErrors.add("Multiple values provided for key `"
                                   + key
                                   + "`; expected one.");
            } else {
              owner = values.get(0);
            }
            break;
          case "file":
            if (values.size() > 0) {
              validationErrors.add("Key `"
                                   + key
                                   + "` does not contain an upload file (needs HTTP multipart `file` parameter).");
            } else {
              uploadedFile = ctx.uploadedFile("file");
              if (uploadedFile == null) validationErrors.add("Key `" + key + "` does not contain an upload file.");
            }
            break;
          default:
            validationErrors.add("Unknown key `" + key + "`");
            break;
        }
      }

      if (name == null) validationErrors.add("No value provided for key `name`");
      if (version == null) validationErrors.add("No value provided for key `version`");
      if (mission == null) validationErrors.add("No value provided for key `mission`");
      if (owner == null) validationErrors.add("No value provided for key `owner`");
      if (uploadedFile == null) validationErrors.add("No upload file provided for key `file`");

      // TODO: Throw an InvalidEntityException instead, once it supports capturing fine-grained information
      //   about where in the entity body the failures occur.
      if (!validationErrors.isEmpty()) throw new NewAdaptationValidationException(
          "Validation failed",
          validationErrors);
    }

    return NewAdaptation.builder()
                        .setName(ctx.formParam("name"))
                        .setVersion(ctx.formParam("version"))
                        .setMission(ctx.formParam("mission"))
                        .setOwner(ctx.formParam("owner"))
                        .setJarSource(uploadedFile.getContent())
                        .build();
  }

  private <T> T parseJson(final String subject, final JsonParser<T> parser)
  throws InvalidJsonException, InvalidEntityException
  {
    try {
      final var requestJson = Json.createReader(new StringReader(subject)).readValue();
      final var result = parser.parse(requestJson);
      return result.getSuccessOrThrow(() -> new InvalidEntityException(List.of(result.failureReason())));
    } catch (JsonParsingException e) {
      throw new InvalidJsonException(e);
    }
  }
}
