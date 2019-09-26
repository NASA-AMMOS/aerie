package gov.nasa.jpl.ammos.mpsa.aerie.plan.http;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

public final class PlanBindings {
  private final IPlanController appController;

  public PlanBindings(final IPlanController appController) {
    this.appController = appController;
  }

  public void registerRoutes(final Javalin javalin) {
    javalin.routes(() -> {
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
        });
      });
    });

    javalin.exception(JsonbException.class, (ex, ctx) -> {
      ctx.status(400).result(JsonbBuilder.create().toJson(List.of(ex.getMessage())));
    }).exception(ValidationException.class, (ex, ctx) -> {
      ctx.status(400).result(JsonbBuilder.create().toJson(ex.getValidationErrors()));
    }).exception(NoSuchPlanException.class, (ex, ctx) -> {
      ctx.status(404);
    }).exception(NoSuchActivityInstanceException.class, (ex, ctx) -> {
      ctx.status(404);
    });
  }

  private void getPlans(final Context ctx) {
    final Map<String, Plan> plans = this.appController
        .getPlans()
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    ctx.result(JsonbBuilder.create().toJson(plans)).contentType("application/json");
  }

  private void getPlan(final Context ctx) throws NoSuchPlanException {
    final String planId = ctx.pathParam("planId");

    final Plan plan = this.appController.getPlanById(planId);

    ctx.result(JsonbBuilder.create().toJson(plan)).contentType("application/json");
  }

  private void postPlan(final Context ctx) throws ValidationException {
    final NewPlan plan = JsonbBuilder.create().fromJson(ctx.body(), NewPlan.class);

    final String planId = this.appController.addPlan(plan);

    ctx.result(planId);
  }

  private void putPlan(final Context ctx) throws ValidationException, NoSuchPlanException {
    final String planId = ctx.pathParam("planId");
    final NewPlan plan = JsonbBuilder.create().fromJson(ctx.body(), NewPlan.class);

    this.appController.replacePlan(planId, plan);
  }

  private void patchPlan(final Context ctx) throws ValidationException, NoSuchPlanException {
    final String planId = ctx.pathParam("planId");
    final Plan patch = JsonbBuilder.create().fromJson(ctx.body(), Plan.class);

    this.appController.updatePlan(planId, patch);
  }

  private void deletePlan(final Context ctx) throws NoSuchPlanException {
    final String planId = ctx.pathParam("planId");

    this.appController.removePlan(planId);
  }

  private void getActivityInstances(final Context ctx) throws NoSuchPlanException {
    final String planId = ctx.pathParam("planId");

    final Plan plan = this.appController.getPlanById(planId);

    ctx.result(JsonbBuilder.create().toJson(plan.activityInstances)).contentType("application/json");
  }

  private void postActivityInstances(final Context ctx) throws ValidationException, NoSuchPlanException {
    final Type ACTIVITY_LIST_TYPE = new ArrayList<ActivityInstance>(){}.getClass().getGenericSuperclass();

    final String planId = ctx.pathParam("planId");
    final List<ActivityInstance> activityInstances = JsonbBuilder.create().fromJson(ctx.body(), ACTIVITY_LIST_TYPE);

    final List<String> activityInstanceIds = this.appController.addActivityInstancesToPlan(planId, activityInstances);

    ctx.result(JsonbBuilder.create().toJson(activityInstanceIds)).contentType("application/json");
  }

  private void getActivityInstance(final Context ctx) throws NoSuchPlanException, NoSuchActivityInstanceException {
    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");

    final ActivityInstance activityInstance = this.appController.getActivityInstanceById(planId, activityInstanceId);

    ctx.result(JsonbBuilder.create().toJson(activityInstance)).contentType("application/json");
  }

  private void putActivityInstance(final Context ctx) throws NoSuchPlanException, ValidationException, NoSuchActivityInstanceException {
    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");
    final ActivityInstance activityInstance = JsonbBuilder.create().fromJson(ctx.body(), ActivityInstance.class);

    this.appController.replaceActivityInstance(planId, activityInstanceId, activityInstance);
  }

  private void patchActivityInstance(final Context ctx) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");
    final ActivityInstance activityInstance = JsonbBuilder.create().fromJson(ctx.body(), ActivityInstance.class);

    this.appController.updateActivityInstance(planId, activityInstanceId, activityInstance);
  }

  private void deleteActivityInstance(final Context ctx) throws NoSuchPlanException, NoSuchActivityInstanceException {
    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");

    this.appController.removeActivityInstanceById(planId, activityInstanceId);
  }
}
