package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks.MockAdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks.MockPlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository;

import java.util.ArrayList;
import java.util.Map;

public final class Fixtures {
  public final MockPlanRepository planRepository;
  public final MockAdaptationService adaptationService;

  public final String EXISTENT_ADAPTATION_ID;
  public final String EXISTENT_PLAN_ID;
  public final String EXISTENT_ACTIVITY_TYPE_ID;
  public final String EXISTENT_ACTIVITY_INSTANCE_ID;
  public final String NONEXISTENT_ADAPTATION_ID;
  public final String NONEXISTENT_PLAN_ID;
  public final String NONEXISTENT_ACTIVITY_TYPE_ID;
  public final String NONEXISTENT_ACTIVITY_INSTANCE_ID;

  public Fixtures() {
    this.planRepository = new MockPlanRepository();
    this.adaptationService = new MockAdaptationService();

    this.NONEXISTENT_ACTIVITY_TYPE_ID = "nonexistent activity type";
    this.EXISTENT_ACTIVITY_TYPE_ID = "existent activity type";

    this.NONEXISTENT_ADAPTATION_ID = "nonexistent adaptation";
    this.EXISTENT_ADAPTATION_ID = this.adaptationService.addAdaptation(Map.of(
        EXISTENT_ACTIVITY_TYPE_ID, new ActivityType()
    ));

    {
      final PlanRepository.PlanTransaction transaction = this.planRepository.newPlan()
          .setName("plan 1")
          .setAdaptationId(this.EXISTENT_ADAPTATION_ID)
          .setStartTimestamp("0000-111T22:33:44")
          .setEndTimestamp("1111-222T33:44:55");

      this.NONEXISTENT_ACTIVITY_INSTANCE_ID = "nonexistent activity";
      this.EXISTENT_ACTIVITY_INSTANCE_ID = transaction.newActivity()
          .setType(this.EXISTENT_ACTIVITY_TYPE_ID)
          .save();

      this.NONEXISTENT_PLAN_ID = "nonexistent plan";
      this.EXISTENT_PLAN_ID = transaction.save();
    }

    this.planRepository.newPlan()
        .setName("plan 1")
        .setAdaptationId(this.EXISTENT_ADAPTATION_ID)
        .setStartTimestamp("0000-111T22:33:44")
        .setEndTimestamp("1111-222T33:44:55")
        .save();

    this.planRepository.newPlan()
        .setName("plan 1")
        .setAdaptationId(this.EXISTENT_ADAPTATION_ID)
        .setStartTimestamp("0000-111T22:33:44")
        .setEndTimestamp("1111-222T33:44:55")
        .save();
  }

  public NewPlan createValidNewPlan() {
    final NewPlan plan = new NewPlan();

    plan.adaptationId = this.EXISTENT_ADAPTATION_ID;
    plan.name = "plan";
    plan.startTimestamp = "0000-111T22:33:44";
    plan.endTimestamp = "1111-222T33:44:55";
    plan.activityInstances = new ArrayList<>();

    return plan;
  }
}
