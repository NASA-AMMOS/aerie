package gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.IPlanController.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks.Fixtures;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository.PlanTransaction;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PlanControllerTest {
  @Test
  public void shouldGetAllPlans() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final List<Pair<String, Plan>> expectedPlans = fixtures.planRepository
        .getAllPlans()
        .map(t -> Pair.of(t.getId(), t.get()))
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
    final Plan expectedPlan = fixtures.planRepository.getPlan(planId).get().get();

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
  public void shouldAddPlan() throws ValidationException {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan plan = fixtures.createValidNewPlan();

    // WHEN
    final String planId = controller.addPlan(plan);

    // THEN
    assertThat(fixtures.planRepository.getPlan(planId)).isPresent();
  }

  @Test
  public void shouldNotAddPlanWithNoAdaptationId() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final NewPlan plan = fixtures.createValidNewPlan();
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

    final NewPlan plan = fixtures.createValidNewPlan();
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
    final NewPlan replacementPlan = fixtures.createValidNewPlan();
    replacementPlan.name += "-replaced";

    controller.replacePlan(planId, replacementPlan);

    // THEN
    final Plan retrievedPlan = fixtures.planRepository.getPlan(planId).get().get();
    assertThat(retrievedPlan.name).isEqualTo(replacementPlan.name);
  }

  @Test
  public void shouldNotReplaceInvalidPlan() {
    // GIVEN
    final Fixtures fixtures = new Fixtures();
    final IPlanController controller = new PlanController(fixtures.planRepository, fixtures.adaptationService);

    final String planId = fixtures.EXISTENT_PLAN_ID;
    final PlanTransaction transaction = fixtures.planRepository.getPlan(planId).get();
    final NewPlan replacementPlan = new NewPlan(transaction.get());
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
    final NewPlan plan = fixtures.createValidNewPlan();

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
    final PlanTransaction transaction = fixtures.planRepository.getPlan(planId).get();

    // WHEN
    final Plan patch = new Plan();
    patch.name = transaction.get().name + "-patched";

    controller.updatePlan(planId, patch);

    // THEN
    assertThat(fixtures.planRepository.getPlan(planId).get().get().name).isEqualTo(patch.name);
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
    assertThat(fixtures.planRepository.getPlan(planId)).isNotPresent();
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
}
