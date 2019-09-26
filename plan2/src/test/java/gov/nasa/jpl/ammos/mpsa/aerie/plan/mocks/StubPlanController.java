package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class StubPlanController implements IPlanController {
  public static final String EXISTENT_PLAN_ID = "abc";
  public static final String NONEXISTENT_PLAN_ID = "def";
  public static final Plan EXISTENT_PLAN;
  public static final NewPlan VALID_NEW_PLAN;
  public static final NewPlan INVALID_NEW_PLAN;
  public static final Plan VALID_PATCH;
  public static final Plan INVALID_PATCH;

  public static final String EXISTENT_ACTIVITY_ID = "activity";
  public static final String NONEXISTENT_ACTIVITY_ID = "no-activity";
  public static final ActivityInstance EXISTENT_ACTIVITY;
  public static final ActivityInstance VALID_ACTIVITY;
  public static final ActivityInstance INVALID_ACTIVITY;

  static {
    EXISTENT_ACTIVITY = new ActivityInstance();
    EXISTENT_ACTIVITY.type = "existent activity";

    VALID_ACTIVITY = new ActivityInstance();
    VALID_ACTIVITY.type = "valid activity";

    INVALID_ACTIVITY = new ActivityInstance();
    INVALID_ACTIVITY.type = "invalid activity";

    VALID_NEW_PLAN = new NewPlan();
    VALID_NEW_PLAN.name = "valid";

    INVALID_NEW_PLAN = new NewPlan();
    INVALID_NEW_PLAN.name = "invalid";

    EXISTENT_PLAN = new Plan();
    EXISTENT_PLAN.name = "existent";
    EXISTENT_PLAN.activityInstances = Map.of(EXISTENT_ACTIVITY_ID, EXISTENT_ACTIVITY);

    VALID_PATCH = new Plan();
    VALID_PATCH.name = "valid patch";

    INVALID_PATCH = new Plan();
    INVALID_PATCH.name = "invalid patch";
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
      throw new ValidationException("invalid new plan", List.of("an error"));
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
      throw new ValidationException("invalid patch", List.of("an error"));
    }
  }

  @Override
  public void replacePlan(final String id, final NewPlan plan) throws ValidationException, NoSuchPlanException {
    if (!Objects.equals(id, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(id);
    } else if (plan.equals(INVALID_NEW_PLAN)) {
      throw new ValidationException("invalid new plan", List.of("an error"));
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
        throw new ValidationException("invalid new activity instances", List.of("an error"));
      }

      activityIds.add(EXISTENT_ACTIVITY_ID);
    }

    return activityIds;
  }

  @Override
  public void removeActivityInstanceById(String planId, String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    }
  }

  @Override
  public void updateActivityInstance(String planId, String activityInstanceId, ActivityInstance patch) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    } else if (Objects.equals(patch, INVALID_ACTIVITY)) {
      throw new ValidationException("invalid patch", List.of("an error"));
    }
  }

  @Override
  public void replaceActivityInstance(String planId, String activityInstanceId, ActivityInstance activityInstance) throws NoSuchPlanException, ValidationException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    } else if (Objects.equals(activityInstance, INVALID_ACTIVITY)) {
      throw new ValidationException("invalid activity instance", List.of("an error"));
    }
  }
}
