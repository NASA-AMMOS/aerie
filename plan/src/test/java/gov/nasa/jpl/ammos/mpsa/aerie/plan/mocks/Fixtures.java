package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class Fixtures {
  public final MockPlanRepository planRepository;
  public final MockAdaptationService adaptationService;

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
      this.adaptationService = new MockAdaptationService();

      this.NONEXISTENT_ACTIVITY_TYPE_ID = "nonexistent activity type";
      this.EXISTENT_ACTIVITY_TYPE_ID = "existent activity type";

      this.NONEXISTENT_ADAPTATION_ID = "nonexistent adaptation";
      this.EXISTENT_ADAPTATION_ID = this.adaptationService.addAdaptation(Map.of(
          EXISTENT_ACTIVITY_TYPE_ID, Map.of("abc", ParameterSchema.STRING)
      ));

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
    plan.startTimestamp = "0000-111T22:33:44";
    plan.endTimestamp = "1111-222T33:44:55";
    plan.activityInstances = new ArrayList<>();

    return plan;
  }

  public ActivityInstance createValidActivityInstance() {
    final ActivityInstance activityInstance = new ActivityInstance();

    activityInstance.type = this.EXISTENT_ACTIVITY_TYPE_ID;
    activityInstance.startTimestamp = "0000-111T22:33:44";
    activityInstance.parameters = new HashMap<>();
    activityInstance.parameters.put("abc", SerializedParameter.of("param-value"));

    return activityInstance;
  }
}
