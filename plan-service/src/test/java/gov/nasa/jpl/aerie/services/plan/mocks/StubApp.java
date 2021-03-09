package gov.nasa.jpl.aerie.services.plan.mocks;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.services.plan.controllers.App;
import gov.nasa.jpl.aerie.services.plan.controllers.Breadcrumb;
import gov.nasa.jpl.aerie.services.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.services.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.services.plan.exceptions.ValidationException;
import gov.nasa.jpl.aerie.services.plan.models.ActivityInstance;
import gov.nasa.jpl.aerie.services.plan.models.NewPlan;
import gov.nasa.jpl.aerie.services.plan.models.Plan;
import gov.nasa.jpl.aerie.services.plan.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class StubApp implements App {
  public static final String EXISTENT_PLAN_ID = "abc";
  public static final String NONEXISTENT_PLAN_ID = "def";
  public static final Plan EXISTENT_PLAN;
  public static final NewPlan VALID_NEW_PLAN;
  public static final JsonValue VALID_NEW_PLAN_JSON;
  public static final NewPlan INVALID_NEW_PLAN;
  public static final JsonValue INVALID_NEW_PLAN_JSON;
  public static final Plan VALID_PATCH;
  public static final JsonValue VALID_PATCH_JSON;
  public static final Plan INVALID_PATCH;
  public static final JsonValue INVALID_PATCH_JSON;

  public static final String EXISTENT_ACTIVITY_ID = "activity";
  public static final String NONEXISTENT_ACTIVITY_ID = "no-activity";
  public static final ActivityInstance EXISTENT_ACTIVITY;
  public static final ActivityInstance VALID_ACTIVITY;
  public static final JsonValue VALID_ACTIVITY_JSON;
  public static final JsonValue VALID_ACTIVITY_LIST_JSON;
  public static final ActivityInstance INVALID_ACTIVITY;
  public static final JsonValue INVALID_ACTIVITY_JSON;
  public static final JsonValue INVALID_ACTIVITY_LIST_JSON;

  public static final List<Pair<List<Breadcrumb>, String>> VALIDATION_ERRORS = List.of(
      Pair.of(List.of(Breadcrumb.of("breadcrumb"), Breadcrumb.of(0)), "an error")
  );

  static {
    EXISTENT_ACTIVITY = new ActivityInstance();
    EXISTENT_ACTIVITY.type = "existent activity";
    EXISTENT_ACTIVITY.startTimestamp = Timestamp.fromString("2016-123T14:25:36");
    EXISTENT_ACTIVITY.parameters = Map.of(
        "abc", SerializedValue.of("test-param")
    );

    VALID_ACTIVITY = new ActivityInstance();
    VALID_ACTIVITY.type = "valid activity";
    VALID_ACTIVITY.startTimestamp = Timestamp.fromString("2018-331T04:00:00");
    VALID_ACTIVITY.parameters = Map.of();

    VALID_ACTIVITY_JSON = Json.createObjectBuilder()
        .add("type", VALID_ACTIVITY.type)
        .add("startTimestamp", VALID_ACTIVITY.startTimestamp.toString())
        .add("parameters", Json.createObjectBuilder().build())
        .build();

    VALID_ACTIVITY_LIST_JSON = Json.createArrayBuilder()
        .add(VALID_ACTIVITY_JSON)
        .build();

    INVALID_ACTIVITY = new  ActivityInstance();
    INVALID_ACTIVITY.type = "invalid activity";

    INVALID_ACTIVITY_JSON = Json.createObjectBuilder()
        .add("type", INVALID_ACTIVITY.type)
        .build();

    INVALID_ACTIVITY_LIST_JSON = Json.createArrayBuilder()
        .add(INVALID_ACTIVITY_JSON)
        .build();

    VALID_NEW_PLAN = new NewPlan();
    VALID_NEW_PLAN.name = "valid";
    VALID_NEW_PLAN.adaptationId = "adaptation id";
    VALID_NEW_PLAN.startTimestamp = Timestamp.fromString("2020-260T04:30:02");
    VALID_NEW_PLAN.endTimestamp = Timestamp.fromString("2019-147T06:09:01");
    VALID_NEW_PLAN.activityInstances = List.of();

    VALID_NEW_PLAN_JSON = Json.createObjectBuilder()
        .add("name", VALID_NEW_PLAN.name)
        .add("adaptationId", VALID_NEW_PLAN.adaptationId)
        .add("startTimestamp", VALID_NEW_PLAN.startTimestamp.toString())
        .add("endTimestamp", VALID_NEW_PLAN.endTimestamp.toString())
        .add("activityInstances", Json.createArrayBuilder().build())
        .build();

    INVALID_NEW_PLAN = new NewPlan();
    INVALID_NEW_PLAN.name = "invalid";

    INVALID_NEW_PLAN_JSON = Json.createObjectBuilder()
        .add("name", INVALID_NEW_PLAN.name)
        .build();

    EXISTENT_PLAN = new Plan();
    EXISTENT_PLAN.name = "existent";
    EXISTENT_PLAN.activityInstances = Map.of(EXISTENT_ACTIVITY_ID, EXISTENT_ACTIVITY);

    VALID_PATCH = new Plan();
    VALID_PATCH.name = "valid patch";

    VALID_PATCH_JSON = Json.createObjectBuilder()
        .add("name", VALID_PATCH.name)
        .build();

    INVALID_PATCH = new Plan();
    INVALID_PATCH.name = "invalid patch";

    INVALID_PATCH_JSON = Json.createObjectBuilder()
        .add("name", INVALID_PATCH.name)
        .build();
  }


  public Stream<Pair<String, Plan>> getPlans() {
    return Stream.of(Pair.of(EXISTENT_PLAN_ID, EXISTENT_PLAN));
  }

  public Plan getPlanById(final String id) throws NoSuchPlanException {
    if (!Objects.equals(id, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(id);
    }

    return EXISTENT_PLAN;
  }

  public String addPlan(final NewPlan plan) throws ValidationException {
    if (plan.equals(INVALID_NEW_PLAN)) {
      throw new ValidationException(VALIDATION_ERRORS);
    }

    return EXISTENT_PLAN_ID;
  }

  @Override
  public void removePlan(final String id) throws NoSuchPlanException {
    if (!Objects.equals(id, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(id);
    }
  }

  @Override
  public void updatePlan(final String id, final Plan patch) throws ValidationException, NoSuchPlanException {
    if (!Objects.equals(id, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(id);
    } else if (Objects.equals(patch, INVALID_PATCH)) {
      throw new ValidationException(VALIDATION_ERRORS);
    }
  }

  @Override
  public void replacePlan(final String id, final NewPlan plan) throws ValidationException, NoSuchPlanException {
    if (!Objects.equals(id, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(id);
    } else if (plan.equals(INVALID_NEW_PLAN)) {
      throw new ValidationException(VALIDATION_ERRORS);
    }
  }

  @Override
  public ActivityInstance getActivityInstanceById(final String planId, final String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    }

    return EXISTENT_ACTIVITY;
  }

  @Override
  public List<String> addActivityInstancesToPlan(final String planId, final List<ActivityInstance> activityInstances) throws ValidationException, NoSuchPlanException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    }

    final List<String> activityIds = new ArrayList<>();
    for (final ActivityInstance activityInstance : activityInstances) {
      if (!Objects.equals(activityInstance, VALID_ACTIVITY)) {
        throw new ValidationException(VALIDATION_ERRORS);
      }

      activityIds.add(EXISTENT_ACTIVITY_ID);
    }

    return activityIds;
  }

  @Override
  public void removeActivityInstanceById(final String planId, final String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    }
  }

  @Override
  public void updateActivityInstance(final String planId, final String activityInstanceId, final ActivityInstance patch) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    } else if (Objects.equals(patch, INVALID_ACTIVITY)) {
      throw new ValidationException(VALIDATION_ERRORS);
    }
  }

  @Override
  public void replaceActivityInstance(final String planId, final String activityInstanceId, final ActivityInstance activityInstance) throws NoSuchPlanException, ValidationException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    } else if (Objects.equals(activityInstance, INVALID_ACTIVITY)) {
      throw new ValidationException(VALIDATION_ERRORS);
    }
  }

  @Override
  public Pair<Instant, JsonValue> getSimulationResultsForPlan(final String planId) throws NoSuchPlanException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    }

    return Pair.of(Instant.EPOCH, JsonValue.EMPTY_JSON_OBJECT);
  }
}
