package gov.nasa.jpl.ammos.mpsa.aerie.plan.http;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.CreatedEntity;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;
import io.javalin.http.Context;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

public final class PlanBindings implements Plugin {
  private final IPlanController appController;

  public PlanBindings(final IPlanController appController) {
    this.appController = appController;
  }

  @Override
  public void apply(final Javalin javalin) {
    javalin.routes(() -> {
      path("plans", () -> {
        before(ctx -> ctx.contentType("application/json"));

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
        });
      });
    });

    javalin.exception(JsonParsingException.class, (ex, ctx) -> ctx
        // If the request body entity is not a legal JsonValue, then this exception is expected.
        .status(400)
        .result(ResponseSerializers.serializeJsonParsingException(ex).toString())
    ).exception(InvalidEntityException.class, (ex, ctx) -> ctx
        // If the request body entity is a legal JsonValue but not a legal object of the type we expect, then this exception
        // is expected.
        .status(400)
        .result(ResponseSerializers.serializeInvalidEntityException(ex).toString())
    ).exception(ValidationException.class, (ex, ctx) -> ctx
        // If the request body entity is a legal JsonNValue and can be successfully converted into a legal object of
        // the type we expect, but that object itself is not appropriate for the context in which it is applied, then
        // this exception is expected.
        .status(422)
        .result(ResponseSerializers.serializeValidationException(ex).toString())
    ).exception(NoSuchPlanException.class, (ex, ctx) -> ctx
        // If a request is made to an entity that doesn't exist (and hence cannot serve the request), then this exception
        // is expected.
        .status(404)
        .result(ResponseSerializers.serializeNoSuchPlanException(ex).toString())
    ).exception(NoSuchActivityInstanceException.class, (ex, ctx) -> ctx
        // If a request is made to an entity that doesn't exist (and hence cannot serve the request), then this exception
        // is expected.
        .status(404)
        .result(ResponseSerializers.serializeNoSuchActivityInstanceException(ex).toString())
    );
  }

  private void getPlans(final Context ctx) {
    final Map<String, Plan> plans = this.appController
        .getPlans()
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    ctx.result(ResponseSerializers.serializePlanMap(plans).toString());
  }

  private void getPlan(final Context ctx) throws NoSuchPlanException {
    final String planId = ctx.pathParam("planId");

    final Plan plan = this.appController.getPlanById(planId);

    ctx.result(ResponseSerializers.serializePlan(plan).toString());
  }

  private void postPlan(final Context ctx) throws ValidationException, InvalidEntityException {
    final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();
    final NewPlan plan = RequestDeserializers.deserializeNewPlan(requestJson);

    final String planId = this.appController.addPlan(plan);

    ctx
        .status(201)
        .header("Location", "/plans/" + planId)
        .result(ResponseSerializers.serializeCreatedEntity(new CreatedEntity(planId)).toString());
  }

  private void putPlan(final Context ctx) throws ValidationException, NoSuchPlanException, InvalidEntityException {
    final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();

    final String planId = ctx.pathParam("planId");
    final NewPlan plan = RequestDeserializers.deserializeNewPlan(requestJson);

    this.appController.replacePlan(planId, plan);
  }

  private void patchPlan(final Context ctx) throws ValidationException, NoSuchPlanException, InvalidEntityException, NoSuchActivityInstanceException {
    final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();

    final String planId = ctx.pathParam("planId");
    final Plan patch = RequestDeserializers.deserializePlanPatch(requestJson);

    this.appController.updatePlan(planId, patch);
  }

  private void deletePlan(final Context ctx) throws NoSuchPlanException {
    final String planId = ctx.pathParam("planId");

    this.appController.removePlan(planId);
  }

  private void getActivityInstances(final Context ctx) throws NoSuchPlanException {
    final String planId = ctx.pathParam("planId");

    final Plan plan = this.appController.getPlanById(planId);

    ctx.result(ResponseSerializers.serializeActivityInstanceMap(plan.activityInstances).toString());
  }

  private void postActivityInstances(final Context ctx) throws ValidationException, NoSuchPlanException, InvalidEntityException {
    final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();

    final String planId = ctx.pathParam("planId");
    final List<ActivityInstance> activityInstances = RequestDeserializers.deserializeActivityInstanceList(requestJson);

    final List<String> activityInstanceIds = this.appController.addActivityInstancesToPlan(planId, activityInstances);

    ctx.result(ResponseSerializers.serializeStringList(activityInstanceIds).toString());
  }

  private void getActivityInstance(final Context ctx) throws NoSuchPlanException, NoSuchActivityInstanceException {
    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");

    final ActivityInstance activityInstance = this.appController.getActivityInstanceById(planId, activityInstanceId);

    ctx.result(ResponseSerializers.serializeActivityInstance(activityInstance).toString());
  }

  private void putActivityInstance(final Context ctx) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException, InvalidEntityException {
    final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();

    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");
    final ActivityInstance activityInstance = RequestDeserializers.deserializeActivityInstance(requestJson);

    this.appController.replaceActivityInstance(planId, activityInstanceId, activityInstance);
  }

  private void patchActivityInstance(final Context ctx) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException, InvalidEntityException {
    final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();

    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");
    final ActivityInstance activityInstance = RequestDeserializers.deserializeActivityInstancePatch(requestJson);

    this.appController.updateActivityInstance(planId, activityInstanceId, activityInstance);
  }

  private void deleteActivityInstance(final Context ctx) throws NoSuchPlanException, NoSuchActivityInstanceException {
    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");

    this.appController.removeActivityInstanceById(planId, activityInstanceId);
  }
}
