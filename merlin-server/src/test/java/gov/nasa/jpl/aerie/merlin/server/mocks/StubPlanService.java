package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.DatasetId;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.services.PlanService;
import gov.nasa.jpl.aerie.merlin.server.services.RevisionData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StubPlanService implements PlanService {
  public static final PlanId EXISTENT_PLAN_ID = new PlanId(1L);
  public static final Plan EXISTENT_PLAN;
  public static final RevisionData REVISION_DATA =
      new RevisionData() {
        @Override
        public MatchResult matches(final RevisionData other) {
          if (Objects.equals(other, this)) return MatchResult.success();
          return MatchResult.failure("Not the same revision data");
        }
      };

  public static final ActivityDirectiveId EXISTENT_ACTIVITY_ID = new ActivityDirectiveId(10157);
  public static final ActivityDirective EXISTENT_ACTIVITY;

  static {
    EXISTENT_ACTIVITY = new ActivityDirective(
        Duration.ZERO,
        "existent activity",
        Map.of("abc", SerializedValue.of("test-param")),
        null,
        true
    );

    EXISTENT_PLAN = new Plan();
    EXISTENT_PLAN.name = "existent";
    EXISTENT_PLAN.missionModelId = "abc";
    EXISTENT_PLAN.activityDirectives = Map.of(EXISTENT_ACTIVITY_ID, EXISTENT_ACTIVITY);
  }


  public Plan getPlanForSimulation(final PlanId planId) throws NoSuchPlanException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    }

    return EXISTENT_PLAN;
  }

   public Plan getPlanForValidation(final PlanId planId) throws NoSuchPlanException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    }

    return EXISTENT_PLAN;
  }

  @Override
  public RevisionData getPlanRevisionData(final PlanId planId) throws NoSuchPlanException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    }

    return REVISION_DATA;
  }

  @Override
  public Map<String, Constraint> getConstraintsForPlan(final PlanId planId)
  throws NoSuchPlanException {
    return Map.of();
  }

  @Override
  public long addExternalDataset(final PlanId planId, final Timestamp datasetStart, final ProfileSet profileSet)
  throws NoSuchPlanException
  {
    return 0;
  }

  @Override
  public void extendExternalDataset(final DatasetId datasetId, final ProfileSet profileSet) {
    throw new UnsupportedOperationException("StubPlanService does not store external datasets, so they cannot be extended");
  }

  @Override
  public List<Pair<Duration, ProfileSet>> getExternalDatasets(final PlanId planId) throws NoSuchPlanException {
    return List.of();
  }

  @Override
  public Map<String, ValueSchema> getExternalResourceSchemas(final PlanId planId) throws NoSuchPlanException {
    return Map.of("external resource", ValueSchema.BOOLEAN);
  }

}
