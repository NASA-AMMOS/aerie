package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.services.PlanService;
import gov.nasa.jpl.aerie.merlin.server.services.RevisionData;

import java.util.Map;
import java.util.Objects;

public final class StubPlanService implements PlanService {
  public static final String EXISTENT_PLAN_ID = "abc";
  public static final Plan EXISTENT_PLAN;
  public static final RevisionData REVISION_DATA =
      new RevisionData() {
        @Override
        public MatchResult matches(final RevisionData other) {
          if (Objects.equals(other, this)) return MatchResult.success();
          return MatchResult.failure("Not the same revision data");
        }
      };

  public static final ActivityInstanceId EXISTENT_ACTIVITY_ID = new ActivityInstanceId(10157);
  public static final ActivityInstance EXISTENT_ACTIVITY;

  static {
    EXISTENT_ACTIVITY = new ActivityInstance();
    EXISTENT_ACTIVITY.type = "existent activity";
    EXISTENT_ACTIVITY.startTimestamp = Timestamp.fromString("2016-123T14:25:36");
    EXISTENT_ACTIVITY.parameters = Map.of(
        "abc", SerializedValue.of("test-param")
    );

    EXISTENT_PLAN = new Plan();
    EXISTENT_PLAN.name = "existent";
    EXISTENT_PLAN.activityInstances = Map.of(EXISTENT_ACTIVITY_ID, EXISTENT_ACTIVITY);
  }


  public Plan getPlan(final String planId) throws NoSuchPlanException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    }

    return EXISTENT_PLAN;
  }

  @Override
  public RevisionData getPlanRevisionData(final String planId) throws NoSuchPlanException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    }

    return REVISION_DATA;
  }

  @Override
  public Map<String, Constraint> getConstraintsForPlan(final String planId)
  throws NoSuchPlanException {
    return Map.of();
  }

  @Override
  public long addExternalDataset(final String id, final Timestamp datasetStart, final ProfileSet profileSet)
  throws NoSuchPlanException
  {
    return 0;
  }

}
