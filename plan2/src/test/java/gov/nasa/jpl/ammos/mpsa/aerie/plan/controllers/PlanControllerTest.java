package gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks.Fixtures;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PlanControllerTest {
  @Test
  public void shouldGetAllPlans() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final List<Pair<String, Plan>> expectedPlans = fixtures.planRepository
        .getAllPlans()
        .collect(Collectors.toUnmodifiableList());

    // WHEN
    final List<Pair<String, Plan>> plans = controller.getPlans().collect(Collectors.toUnmodifiableList());

    // THEN
    assertThat(plans).isEqualTo(expectedPlans);
  }

  @Test
  public void shouldGetPlanById() throws NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.getPlanById(planId));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);

    final String invalidPlanId = ((NoSuchPlanException)thrown).getInvalidPlanId();
    assertThat(invalidPlanId).isEqualTo(planId);
  }

  @Test
  public void shouldAddPlan() throws ValidationException, NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan plan = fixtures.createValidNewPlan("new-plan");
    plan.adaptationId = null;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.addPlan(plan));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final List<String> validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldNotAddPlanWithNoName() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan plan = fixtures.createValidNewPlan("new-plan");
    plan.name = null;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.addPlan(plan));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final List<String> validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldReplacePlan() throws ValidationException, NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final Plan plan = fixtures.planRepository.getPlan(planId);
    final NewPlan replacementPlan = new NewPlan(plan);
    replacementPlan.name = null;

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.replacePlan(planId, replacementPlan));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final List<String> validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldNotReplaceNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
  public void shouldPatchPlan() throws ValidationException, NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
  public void shouldNotPatchNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
  @Disabled("disabled until activity validation is implemented")
  public void shouldNotPatchInvalidPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;

    // WHEN
    final Plan patch = new Plan();
    patch.activityInstances = Map.of(fixtures.NONEXISTENT_ACTIVITY_INSTANCE_ID, new ActivityInstance());

    final Throwable thrown = catchThrowable(() -> controller.updatePlan(planId, patch));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final List<String> validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldRemovePlan() throws NoSuchPlanException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan newPlan = fixtures.createValidNewPlan("new-plan");
    final String planId = fixtures.planRepository.createPlan(newPlan);

    final List<ActivityInstance> activityInstances = List.of(
        fixtures.createValidActivityInstance(),
        fixtures.createValidActivityInstance());

    // WHEN
    final List<String> activityInstanceIds = controller.addActivityInstancesToPlan(planId, activityInstances);

    // THEN
    assertThat(activityInstanceIds).size().isEqualTo(activityInstances.size());
    assertThat(zipToMap(activityInstanceIds, activityInstances))
        .isEqualTo(pairStreamToMap(fixtures.planRepository.getAllActivitiesInPlan(planId)));
  }

  @Test
  public void shouldNotAddActivityInstancesToNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
  @Disabled("disabled until activity validation is implemented")
  public void shouldNotAddInvalidActivityInstancesToPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final ActivityInstance activityInstance = fixtures.createValidActivityInstance();
    activityInstance.type = null;

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final List<ActivityInstance> activityInstances = List.of(activityInstance);

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.addActivityInstancesToPlan(planId, activityInstances));

    // THEN
    assertThat(thrown).isInstanceOf(ValidationException.class);

    final List<String> validationErrors = ((ValidationException)thrown).getValidationErrors();
    assertThat(validationErrors).size().isEqualTo(1);
  }

  @Test
  public void shouldDeleteActivityInstanceById() throws NoSuchPlanException, NoSuchActivityInstanceException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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
  public void shouldReplaceActivityInstance() throws NoSuchPlanException, NoSuchActivityInstanceException, ValidationException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance activityInstance = new ActivityInstance(fixtures.EXISTENT_ACTIVITY_INSTANCE);
    activityInstance.startTimestamp += ".001";

    // WHEN
    controller.replaceActivityInstance(planId, activityInstanceId, activityInstance);

    // THEN
    assertThat(fixtures.planRepository.getActivityInPlanById(planId, activityInstanceId)).isEqualTo(activityInstance);
  }

  @Test
  public void shouldNotReplaceActivityInstanceOfNonexistentPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.NONEXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.EXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance activityInstance = new ActivityInstance(fixtures.EXISTENT_ACTIVITY_INSTANCE);
    activityInstance.startTimestamp += ".001";

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
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final String activityInstanceId = fixtures.NONEXISTENT_ACTIVITY_INSTANCE_ID;
    final ActivityInstance activityInstance = new ActivityInstance(fixtures.EXISTENT_ACTIVITY_INSTANCE);
    activityInstance.startTimestamp += ".001";

    // WHEN
    final Throwable thrown = catchThrowable(() -> controller.replaceActivityInstance(planId, activityInstanceId, activityInstance));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchActivityInstanceException.class);
    assertThat(((NoSuchActivityInstanceException)thrown).getPlanId()).isEqualTo(planId);
    assertThat(((NoSuchActivityInstanceException)thrown).getInvalidActivityId()).isEqualTo(activityInstanceId);
  }

  @Test
  @Disabled("disabled until activity validation is implemented")
  public void shouldNotReplaceInvalidActivityInstance() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

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

  private <K, V> Map<K, V> pairStreamToMap(final Stream<Pair<K, V>> pairs) {
    return pairs.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }
}
