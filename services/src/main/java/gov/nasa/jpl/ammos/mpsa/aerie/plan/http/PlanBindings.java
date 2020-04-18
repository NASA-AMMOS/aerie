package gov.nasa.jpl.ammos.mpsa.aerie.plan.http;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.App;
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
  private final App app;

  public PlanBindings(final App app) {
    this.app = app;
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
          path("results", () -> {
            get(this::getSimulationResults);
          });
        });
      });
    });

    // This exception is expected when the request body entity is not a legal JsonValue.
    javalin.exception(JsonParsingException.class, (ex, ctx) -> ctx
        .status(400)
        .result(ResponseSerializers.serializeJsonParsingException(ex).toString()));
  }

  private void getSimulationResults(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");

      final var results = this.app.getSimulationResultsForPlan(planId);

      ctx.result(ResponseSerializers.serializeSimulationResults(results).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    }
  }

  private void getPlans(final Context ctx) {
    final Map<String, Plan> plans = this.app
        .getPlans()
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    ctx.result(ResponseSerializers.serializePlanMap(plans).toString());
  }

  private void getPlan(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");

      final Plan plan = this.app.getPlanById(planId);

      ctx.result(ResponseSerializers.serializePlan(plan).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    }
  }

  private void postPlan(final Context ctx) {
    try {
      final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();
      final NewPlan plan = RequestDeserializers.deserializeNewPlan(requestJson);

      final String planId = this.app.addPlan(plan);

      ctx
          .status(201)
          .header("Location", "/plans/" + planId)
          .result(ResponseSerializers.serializeCreatedEntity(new CreatedEntity(planId)).toString());
    } catch (final InvalidEntityException ex) {
      ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
    } catch (final ValidationException ex) {
      ctx.status(422).result(ResponseSerializers.serializeValidationException(ex).toString());
    }
  }

  private void putPlan(final Context ctx) {
    try {
      final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();

      final String planId = ctx.pathParam("planId");
      final NewPlan plan = RequestDeserializers.deserializeNewPlan(requestJson);

      this.app.replacePlan(planId, plan);
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
      final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();

      final String planId = ctx.pathParam("planId");
      final Plan patch = RequestDeserializers.deserializePlanPatch(requestJson);

      this.app.updatePlan(planId, patch);
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

      this.app.removePlan(planId);
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    }
  }

  private void getActivityInstances(final Context ctx) {
    try {
      final String planId = ctx.pathParam("planId");

      final Plan plan = this.app.getPlanById(planId);

      ctx.result(ResponseSerializers.serializeActivityInstanceMap(plan.activityInstances).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    }
  }

  private void postActivityInstances(final Context ctx) {
    try {
      final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();

      final String planId = ctx.pathParam("planId");
      final List<ActivityInstance> activityInstances = RequestDeserializers.deserializeActivityInstanceList(requestJson);

      final List<String> activityInstanceIds = this.app.addActivityInstancesToPlan(planId, activityInstances);

      ctx.result(ResponseSerializers.serializeStringList(activityInstanceIds).toString());
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

      final ActivityInstance activityInstance = this.app.getActivityInstanceById(planId, activityInstanceId);

      ctx.result(ResponseSerializers.serializeActivityInstance(activityInstance).toString());
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    } catch (final NoSuchActivityInstanceException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchActivityInstanceException(ex).toString());
    }
  }

  private void putActivityInstance(final Context ctx) {
    try {
      final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();

      final String planId = ctx.pathParam("planId");
      final String activityInstanceId = ctx.pathParam("activityInstanceId");
      final ActivityInstance activityInstance = RequestDeserializers.deserializeActivityInstance(requestJson);

      this.app.replaceActivityInstance(planId, activityInstanceId, activityInstance);
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
      final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();

      final String planId = ctx.pathParam("planId");
      final String activityInstanceId = ctx.pathParam("activityInstanceId");
      final ActivityInstance activityInstance = RequestDeserializers.deserializeActivityInstancePatch(requestJson);

      this.app.updateActivityInstance(planId, activityInstanceId, activityInstance);
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

      this.app.removeActivityInstanceById(planId, activityInstanceId);
    } catch (final NoSuchPlanException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchPlanException(ex).toString());
    } catch (final NoSuchActivityInstanceException ex) {
      ctx.status(404).result(ResponseSerializers.serializeNoSuchActivityInstanceException(ex).toString());
    }
  }
}
