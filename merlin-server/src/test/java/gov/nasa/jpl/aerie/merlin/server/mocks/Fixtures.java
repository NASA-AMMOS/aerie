package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import java.util.ArrayList;

public final class Fixtures {
  public final InMemoryPlanRepository planRepository;
  public final StubMissionModelService missionModelService;

  public final String EXISTENT_MISSION_MODEL_ID;
  public final PlanId EXISTENT_PLAN_ID;
  public final String EXISTENT_ACTIVITY_TYPE_ID;
  public final ActivityDirectiveId EXISTENT_ACTIVITY_INSTANCE_ID;
  public final ActivityDirective EXISTENT_ACTIVITY_INSTANCE;
  public final String NONEXISTENT_MISSION_MODEL_ID;
  public final String NONEXISTENT_PLAN_ID;
  public final String NONEXISTENT_ACTIVITY_TYPE_ID;
  public final String NONEXISTENT_ACTIVITY_INSTANCE_ID;

  public Fixtures() {
    try {
      this.planRepository = new InMemoryPlanRepository();
      this.missionModelService = new StubMissionModelService();

      this.NONEXISTENT_ACTIVITY_TYPE_ID = StubMissionModelService.NONEXISTENT_ACTIVITY_TYPE;
      this.EXISTENT_ACTIVITY_TYPE_ID = StubMissionModelService.EXISTENT_ACTIVITY_TYPE;

      this.NONEXISTENT_MISSION_MODEL_ID = StubMissionModelService.NONEXISTENT_MISSION_MODEL_ID;
      this.EXISTENT_MISSION_MODEL_ID = StubMissionModelService.EXISTENT_MISSION_MODEL_ID;

      this.EXISTENT_PLAN_ID = this.planRepository.createPlan(createValidNewPlan("plan 1")).planId();
      this.NONEXISTENT_PLAN_ID = "nonexistent plan";

      this.planRepository.createPlan(createValidNewPlan("plan 2"));
      this.planRepository.createPlan(createValidNewPlan("plan 3"));

      {
        final ActivityDirective activity = createValidActivityInstance();

        this.EXISTENT_ACTIVITY_INSTANCE = activity;
        this.EXISTENT_ACTIVITY_INSTANCE_ID = this.planRepository.createActivity(this.EXISTENT_PLAN_ID, activity);
        this.NONEXISTENT_ACTIVITY_INSTANCE_ID = "nonexistent activity";
      }
    } catch (final NoSuchPlanException ex) {
      throw new RuntimeException(ex);
    }
  }

  public NewPlan createValidNewPlan(final String name) {
    final NewPlan plan = new NewPlan();

    plan.missionModelId = this.EXISTENT_MISSION_MODEL_ID;
    plan.name = name;
    plan.startTimestamp = Timestamp.fromString("0000-111T22:33:44");
    plan.endTimestamp = Timestamp.fromString("1111-222T00:44:55");
    plan.activityDirectives = new ArrayList<>();

    return plan;
  }

  public ActivityDirective createValidActivityInstance() {
    return new ActivityDirective(
        Duration.ZERO,
        this.EXISTENT_ACTIVITY_TYPE_ID,
        StubMissionModelService.VALID_ACTIVITY_INSTANCE.getArguments()
    );
  }
}
