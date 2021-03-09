package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import java.util.ArrayList;

public final class Fixtures {
  public final MockPlanRepository planRepository;
  public final StubAdaptationService adaptationService;

  public final String EXISTENT_ADAPTATION_ID;
  public final String EXISTENT_PLAN_ID;
  public final String EXISTENT_ACTIVITY_TYPE_ID;
  public final String EXISTENT_ACTIVITY_INSTANCE_ID;
  public final ActivityInstance EXISTENT_ACTIVITY_INSTANCE;
  public final String NONEXISTENT_ADAPTATION_ID;
  public final String NONEXISTENT_PLAN_ID;
  public final String NONEXISTENT_ACTIVITY_TYPE_ID;
  public final String NONEXISTENT_ACTIVITY_INSTANCE_ID;

  public Fixtures() {
    try {
      this.planRepository = new MockPlanRepository();
      this.adaptationService = new StubAdaptationService();

      this.NONEXISTENT_ACTIVITY_TYPE_ID = StubAdaptationService.NONEXISTENT_ACTIVITY_TYPE;
      this.EXISTENT_ACTIVITY_TYPE_ID = StubAdaptationService.EXISTENT_ACTIVITY_TYPE;

      this.NONEXISTENT_ADAPTATION_ID = StubAdaptationService.NONEXISTENT_ADAPTATION_ID;
      this.EXISTENT_ADAPTATION_ID = StubAdaptationService.EXISTENT_ADAPTATION_ID;

      this.EXISTENT_PLAN_ID = this.planRepository.createPlan(createValidNewPlan("plan 1"));
      this.NONEXISTENT_PLAN_ID = "nonexistent plan";

      this.planRepository.createPlan(createValidNewPlan("plan 2"));
      this.planRepository.createPlan(createValidNewPlan("plan 3"));

      {
        final ActivityInstance activity = createValidActivityInstance();

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

    plan.adaptationId = this.EXISTENT_ADAPTATION_ID;
    plan.name = name;
    plan.startTimestamp = Timestamp.fromString("0000-111T22:33:44");
    plan.endTimestamp = Timestamp.fromString("1111-222T00:44:55");
    plan.activityInstances = new ArrayList<>();

    return plan;
  }

  public ActivityInstance createValidActivityInstance() {
    final ActivityInstance activityInstance = new ActivityInstance();

    activityInstance.type = this.EXISTENT_ACTIVITY_TYPE_ID;
    activityInstance.startTimestamp = Timestamp.fromString("0000-111T22:33:44");
    activityInstance.parameters = StubAdaptationService.VALID_ACTIVITY_INSTANCE.getParameters();

    return activityInstance;
  }
}
