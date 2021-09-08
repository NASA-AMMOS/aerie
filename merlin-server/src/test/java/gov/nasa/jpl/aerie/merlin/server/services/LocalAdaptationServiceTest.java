package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.ValidationException;
import gov.nasa.jpl.aerie.merlin.server.mocks.Fixtures;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public final class LocalAdaptationServiceTest {
  @Test
  public void shouldGetAllPlans() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final Map<String, Plan> expectedPlans = fixtures.planRepository.getAllPlans();

    // WHEN
    final Map<String, Plan> plans = controller.getPlans();

    // THEN
    assertThat(plans).isEqualTo(expectedPlans);
  }

  @Test
  public void shouldGetPlanById() throws NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final Plan expectedPlan = fixtures.planRepository.getPlan(planId);

    // WHEN
    final Plan plan = controller.getPlanById(planId);

    // THEN
    assertThat(plan).isEqualTo(expectedPlan);
  }

  @Test
  public void shouldGetNonexistentPlanById() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.getPlanById(planId));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);

    final String invalidPlanId = ((NoSuchPlanException)thrown).getInvalidPlanId();
    assertThat(invalidPlanId).isEqualTo(planId);
  }

  @Test
  public void shouldAddPlan()
  throws ValidationException, NoSuchPlanException
  {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan plan = fixtures.createValidNewPlan("new-plan");

    // WHEN
    final String planId = controller.addPlan(plan);

    // THEN
    assertThat(fixtures.planRepository.getPlan(planId)).isNotNull();
  }

  @Test
  public void shouldNotAddPlanWithNoAdaptationId() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan plan = fixtures.createValidNewPlan("new-plan");
    plan.adaptationId = null;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.addPlan(plan));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final var validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldNotAddPlanWithNonexistantAdaptation() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan plan = fixtures.createValidNewPlan("new-plan");
    plan.adaptationId = fixtures.NONEXISTENT_ADAPTATION_ID;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.addPlan(plan));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final var validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldNotAddPlanWithNoName() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan plan = fixtures.createValidNewPlan("new-plan");
    plan.name = null;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.addPlan(plan));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final var validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldReplacePlan() throws ValidationException, NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;

    // WHEN
    final NewPlan replacementPlan = fixtures.createValidNewPlan("new-plan");
    replacementPlan.name += "-replaced";

    controller.replacePlan(planId, replacementPlan);

    // THEN
    final Plan retrievedPlan = fixtures.planRepository.getPlan(planId);
    assertThat(retrievedPlan.name).isEqualTo(replacementPlan.name);
  }

  @Test
  public void shouldNotReplaceInvalidPlan() throws NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final Plan plan = fixtures.planRepository.getPlan(planId);
    final NewPlan replacementPlan = new NewPlan(plan);
    replacementPlan.name = null;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.replacePlan(planId, replacementPlan));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final var validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldNotReplaceNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;
    final NewPlan plan = fixtures.createValidNewPlan("new-plan");

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.replacePlan(planId, plan));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);

    final String invalidPlanId = ((NoSuchPlanException)thrown).getInvalidPlanId();
    assertThat(invalidPlanId).isEqualTo(planId);
  }

  @Test
  public void shouldPatchPlan() throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final Plan plan = fixtures.planRepository.getPlan(planId);

    // WHEN
    final Plan patch = new Plan();
    patch.name = plan.name + "-patched";

    controller.updatePlan(planId, patch);

    // THEN
    assertThat(fixtures.planRepository.getPlan(planId).name).isEqualTo(patch.name);
  }

  @Test
  public void shouldPatchPlanActivities() throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan newPlan = fixtures.createValidNewPlan("new-plan");
    final String planId = fixtures.planRepository.createPlan(newPlan).planId();

    // Add 3 activity instances to the plan
    final String activity1Id = fixtures.planRepository.createActivity(planId, fixtures.createValidActivityInstance());
    final String activity2Id = fixtures.planRepository.createActivity(planId, fixtures.createValidActivityInstance());
    final ActivityInstance activity3 = fixtures.createValidActivityInstance();
    activity3.startTimestamp = Timestamp.fromString("2018-331T04:00:00");
    final String activity3Id = fixtures.planRepository.createActivity(planId, activity3);

    // Create a unique activity instance to patch activity 2 with
    final ActivityInstance patchedInstance = fixtures.createValidActivityInstance();
    patchedInstance.startTimestamp = Timestamp.fromString("2020-138T22:33:45");

    // WHEN
    // Create the plan patch
    final Plan patch = new Plan();
    Map<String, ActivityInstance> instances = new HashMap<>();
    instances.put(activity1Id, null);
    instances.put(activity2Id, patchedInstance);
    patch.activityInstances = instances;

    controller.updatePlan(planId, patch);

    // THEN

    // Plan metadata should be unchanged
    Plan patchedPlan = fixtures.planRepository.getPlan(planId);
    assertThat(patchedPlan.name).isEqualTo(newPlan.name);
    assertThat(patchedPlan.adaptationId).isEqualTo(newPlan.adaptationId);
    assertThat(patchedPlan.startTimestamp).isEqualTo(newPlan.startTimestamp);
    assertThat(patchedPlan.endTimestamp).isEqualTo(newPlan.endTimestamp);

    // Should contain just 2 activity instances now
    Map<String, ActivityInstance> patchedInstanceList = patchedPlan.activityInstances;
    assertThat(patchedInstanceList.size()).isEqualTo(2);

    // Activity 2 should exist but have a patched start time
    assertThat(patchedInstanceList.get(activity2Id)).isNotNull();
    assertThat(patchedInstanceList.get(activity2Id).startTimestamp).isEqualTo(patchedInstance.startTimestamp);

    // Activity 3 should be exactly the same
    assertThat(patchedInstanceList.get(activity3Id)).isNotNull();
    assertThat(patchedInstanceList.get(activity3Id)).isEqualTo(activity3);
  }

  @Test
  public void shouldNotPatchNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;

    // WHEN
    final Plan patch = new Plan();
    patch.activityInstances = Map.of(fixtures.EXISTENT_ACTIVITY_INSTANCE_ID, new ActivityInstance());

    final Throwable thrown = catchThrowable(() -> controller.updatePlan(planId, patch));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);

    final String invalidPlanId = ((NoSuchPlanException)thrown).getInvalidPlanId();
    assertThat(invalidPlanId).isEqualTo(planId);
  }

  @Test
  public void shouldNotChangeAdaptationOfPlan() throws NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final Plan plan = fixtures.planRepository.getPlan(planId);

    // WHEN
    final Plan patch = new Plan();
    patch.adaptationId = "something";

    final Throwable thrown = catchThrowable(() -> controller.updatePlan(planId, patch));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final var validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldNotPatchInvalidPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;

    // WHEN
    final ActivityInstance activityInstance = new ActivityInstance();
    activityInstance.type = fixtures.NONEXISTENT_ACTIVITY_TYPE_ID;
    activityInstance.parameters = Map.of();

    final Plan patch = new Plan();
    patch.activityInstances = Map.of(fixtures.NONEXISTENT_ACTIVITY_INSTANCE_ID, activityInstance);

    final Throwable thrown = catchThrowable(() -> controller.updatePlan(planId, patch));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final var validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(3);
  }

  @Test
  public void shouldRemovePlan() throws NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;

    // WHEN
    controller.removePlan(planId);

    // THEN
    final Throwable thrown = catchThrowable(() -> fixtures.planRepository.getPlan(planId));
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);
  }

  @Test
  public void shouldNotRemoveNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.removePlan(planId));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);

    final String invalidPlanId = ((NoSuchPlanException)thrown).getInvalidPlanId();
    assertThat(invalidPlanId).isEqualTo(planId);
  }

  @Test
  public void shouldGetActivityInstanceById() throws NoSuchPlanException, NoSuchActivityInstanceException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance expectedActivityInstance = fixtures.EXISTENT_ACTIVITY_INSTANCE;

    // WHEN
    final ActivityInstance activityInstance = controller.getActivityInstanceById(planId, activityInstanceId);

    // THEN
    assertThat(activityInstance).isEqualTo(expectedActivityInstance);
  }

  @Test
  public void shouldNotGetActivityInstanceFromNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.NONEXISTENT_ACTIVITY_INSTANCE_ID;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.getActivityInstanceById(planId, activityInstanceId));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);
    assertThat(((NoSuchPlanException)thrown).getInvalidPlanId()).isEqualTo(planId);
  }

  @Test
  public void shouldNotGetNonexistentActivityInstance() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.NONEXISTENT_ACTIVITY_INSTANCE_ID;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.getActivityInstanceById(planId, activityInstanceId));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchActivityInstanceException.class);
    assertThat(((NoSuchActivityInstanceException)thrown).getPlanId()).isEqualTo(planId);
    assertThat(((NoSuchActivityInstanceException)thrown).getInvalidActivityId()).isEqualTo(activityInstanceId);
  }

  @Test
  public void shouldAddActivityInstancesToPlan() throws ValidationException, NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan newPlan = fixtures.createValidNewPlan("new-plan");
    final String planId = fixtures.planRepository.createPlan(newPlan).planId();

    final List<ActivityInstance> activityInstances = List.of(
        fixtures.createValidActivityInstance(),
        fixtures.createValidActivityInstance());

    // WHEN
    final List<String> activityInstanceIds = controller.addActivityInstancesToPlan(planId, activityInstances);

    // THEN
    assertThat(activityInstanceIds).size().isEqualTo(activityInstances.size());
    assertThat(zipToMap(activityInstanceIds, activityInstances))
        .isEqualTo(fixtures.planRepository.getAllActivitiesInPlan(planId));
  }

  @Test
  public void shouldNotAddActivityInstancesToNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;
    final List<ActivityInstance> activityInstances = List.of(
        fixtures.createValidActivityInstance(),
        fixtures.createValidActivityInstance());

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.addActivityInstancesToPlan(planId, activityInstances));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);

    final String invalidPlanId = ((NoSuchPlanException)thrown).getInvalidPlanId();
    assertThat(invalidPlanId).isEqualTo(planId);
  }

  @Test
  public void shouldNotAddInvalidActivityInstancesToPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final ActivityInstance activityInstance = fixtures.createValidActivityInstance();
    activityInstance.type = fixtures.NONEXISTENT_ACTIVITY_TYPE_ID;

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final List<ActivityInstance> activityInstances = List.of(activityInstance);

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.addActivityInstancesToPlan(planId, activityInstances));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final var validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldDeleteActivityInstanceById() throws NoSuchPlanException, NoSuchActivityInstanceException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;

    // WHEN
    controller.removeActivityInstanceById(planId, activityInstanceId);

    // THEN
    final Throwable thrown = catchThrowable(() -> fixtures.planRepository.getActivityInPlanById(planId, activityInstanceId));
    assertThat(thrown).isInstanceOf(NoSuchActivityInstanceException.class);
    assertThat(((NoSuchActivityInstanceException)thrown).getPlanId()).isEqualTo(planId);
    assertThat(((NoSuchActivityInstanceException)thrown).getInvalidActivityId()).isEqualTo(activityInstanceId);
  }

  @Test
  public void shouldNotDeleteActivityInstanceFromNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.removeActivityInstanceById(planId, activityInstanceId));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);
    assertThat(((NoSuchPlanException)thrown).getInvalidPlanId()).isEqualTo(planId);
  }

  @Test
  public void shouldNotDeleteNonexistentActivityInstance() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.NONEXISTENT_ACTIVITY_INSTANCE_ID;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.removeActivityInstanceById(planId, activityInstanceId));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchActivityInstanceException.class);
    assertThat(((NoSuchActivityInstanceException)thrown).getPlanId()).isEqualTo(planId);
    assertThat(((NoSuchActivityInstanceException)thrown).getInvalidActivityId()).isEqualTo(activityInstanceId);
  }

  @Test
  public void shouldUpdateActivityInstanceById() throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance expectedActivityInstance = new ActivityInstance(fixtures.EXISTENT_ACTIVITY_INSTANCE);

    // WHEN
    final ActivityInstance patch = new ActivityInstance();
    patch.startTimestamp = expectedActivityInstance.startTimestamp;

    controller.updateActivityInstance(planId, activityInstanceId, patch);

    // THEN
    assertThat(fixtures.planRepository.getActivityInPlanById(planId, activityInstanceId)).isEqualTo(expectedActivityInstance);
  }

  @Test
  public void shouldNotUpdateActivityInstanceInNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance expectedActivityInstance = new ActivityInstance(fixtures.EXISTENT_ACTIVITY_INSTANCE);

    // WHEN
    final ActivityInstance patch = new ActivityInstance();
    patch.startTimestamp = expectedActivityInstance.startTimestamp;

    final Throwable thrown = catchThrowable(() -> controller.updateActivityInstance(planId, activityInstanceId, patch));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);
    assertThat(((NoSuchPlanException)thrown).getInvalidPlanId()).isEqualTo(planId);
  }

  @Test
  public void shouldNotUpdateNonexistentActivityInstance() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.NONEXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance expectedActivityInstance = new ActivityInstance(fixtures.EXISTENT_ACTIVITY_INSTANCE);

    // WHEN
    final ActivityInstance patch = new ActivityInstance();
    patch.startTimestamp = expectedActivityInstance.startTimestamp;

    final Throwable thrown = catchThrowable(() -> controller.updateActivityInstance(planId, activityInstanceId, patch));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchActivityInstanceException.class);
    assertThat(((NoSuchActivityInstanceException)thrown).getPlanId()).isEqualTo(planId);
    assertThat(((NoSuchActivityInstanceException)thrown).getInvalidActivityId()).isEqualTo(activityInstanceId);
  }

  @Test
  public void shouldNotUpdateInvalidActivityInstance() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;

    // WHEN
    final ActivityInstance patch = new ActivityInstance();
    patch.type = fixtures.NONEXISTENT_ACTIVITY_TYPE_ID;

    final Throwable thrown = catchThrowable(() -> controller.updateActivityInstance(planId, activityInstanceId, patch));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);
    assertThat(((ValidationException)thrown).getValidationErrors()).size().isEqualTo(1);
  }

  @Test
  public void shouldReplaceActivityInstance() throws NoSuchPlanException, NoSuchActivityInstanceException, ValidationException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance activityInstance = new ActivityInstance(fixtures.EXISTENT_ACTIVITY_INSTANCE);

    // WHEN
    controller.replaceActivityInstance(planId, activityInstanceId, activityInstance);

    // THEN
    assertThat(fixtures.planRepository.getActivityInPlanById(planId, activityInstanceId)).isEqualTo(activityInstance);
  }

  @Test
  public void shouldNotReplaceActivityInstanceOfNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance activityInstance = new ActivityInstance(fixtures.EXISTENT_ACTIVITY_INSTANCE);

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.replaceActivityInstance(planId, activityInstanceId, activityInstance));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);
    assertThat(((NoSuchPlanException)thrown).getInvalidPlanId()).isEqualTo(planId);
  }

  @Test
  public void shouldNotReplaceNonexistentActivityInstance() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.NONEXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance activityInstance = new ActivityInstance(fixtures.EXISTENT_ACTIVITY_INSTANCE);

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.replaceActivityInstance(planId, activityInstanceId, activityInstance));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchActivityInstanceException.class);
    assertThat(((NoSuchActivityInstanceException)thrown).getPlanId()).isEqualTo(planId);
    assertThat(((NoSuchActivityInstanceException)thrown).getInvalidActivityId()).isEqualTo(activityInstanceId);
  }

  @Test
  public void shouldNotReplaceInvalidActivityInstance() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final PlanService controller = new LocalPlanService(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance activityInstance = new ActivityInstance(fixtures.EXISTENT_ACTIVITY_INSTANCE);
    activityInstance.startTimestamp = null;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.replaceActivityInstance(planId, activityInstanceId, activityInstance));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);
    assertThat(((ValidationException)thrown).getValidationErrors()).size().isEqualTo(1);
  }

  private <K, V> Map<K, V> zipToMap(final List<K> keys, final List<V> values) {
    final Map<K, V> map = new HashMap<>();

    final var keysIterator = keys.iterator();
    final var valuesIterator = values.iterator();
    while (keysIterator.hasNext() && valuesIterator.hasNext()) {
      map.put(keysIterator.next(), valuesIterator.next());
    }

    assert !keysIterator.hasNext() && !valuesIterator.hasNext();

    return map;
  }
}
