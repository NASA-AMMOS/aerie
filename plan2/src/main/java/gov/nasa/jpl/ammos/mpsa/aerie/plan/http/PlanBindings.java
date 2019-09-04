package gov.nasa.jpl.ammos.mpsa.aerie.plan.http;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
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

  private void postActivityInstances(final Context ctx) {
    final String planId = ctx.pathParam("planId");
    final String activityInstancesBody = ctx.body();
    ctx.result("postActivityInstances(" + planId + ", body(" + activityInstancesBody.length() + "))");
  }

  private void getActivityInstance(final Context ctx) {
    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");
    ctx.result("getActivityInstance(" + planId + ", " + activityInstanceId + ")");
  }

  private void putActivityInstance(final Context ctx) {
    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");
    final String activityInstanceBody = ctx.body();
    ctx.result("putActivityInstance(" + planId + ", " + activityInstanceId + ", " + activityInstanceBody.length() + ")");
  }

  private void patchActivityInstance(final Context ctx) {
    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");
    final String activityInstanceBody = ctx.body();
    ctx.result("patchActivityInstance(" + planId + ", " + activityInstanceId + ", " + activityInstanceBody.length() + ")");
  }

  private void deleteActivityInstance(final Context ctx) {
    final String planId = ctx.pathParam("planId");
    final String activityInstanceId = ctx.pathParam("activityInstanceId");
    ctx.result("deleteActivityInstance(" + planId + ", " + activityInstanceId + ")");
  }
}
