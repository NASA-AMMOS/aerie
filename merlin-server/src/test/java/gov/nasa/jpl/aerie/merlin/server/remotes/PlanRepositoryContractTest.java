package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.mocks.InMemoryPlanRepository;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository.CreatedPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class PlanRepositoryContractTest {
  protected InMemoryPlanRepository planRepository = null;

  protected abstract void resetRepository();

  @BeforeEach
  public void resetRepositoryBeforeEachTest() {
    this.resetRepository();
  }

  @Test
  public void testCanStorePlan() throws NoSuchPlanException {
    // GIVEN

    // WHEN
    final Plan plan = new Plan();
    plan.name = "new-plan";

    final CreatedPlan ids = this.planRepository.storePlan(plan);

    // THEN
    final Plan fetchedPlan = this.planRepository.getPlanForValidation(ids.planId());
    assertEquals("new-plan", fetchedPlan.name);
  }

  @Test
  public void testCreatePlanWithActivity() throws NoSuchPlanException {
    // GIVEN

    // WHEN
    final ActivityDirective activity = new ActivityDirective(Duration.ZERO, "abc", Map.of("abc", SerializedValue.of(1)), null, true);

    final Plan plan = new Plan();
    plan.name = "new-plan";
    plan.activityDirectives = Map.of();

    final CreatedPlan ids = this.planRepository.storePlan(plan);
    this.planRepository.createActivity(ids.planId(), activity);

    // THEN
    final Plan fetchedPlan = this.planRepository.getPlanForValidation(ids.planId());
    assertEquals("new-plan", fetchedPlan.name);
    assertEquals(1, fetchedPlan.activityDirectives.size());
    assertTrue(fetchedPlan.activityDirectives.containsValue(activity));
  }

  @Test
  public void testCreatePlanWithNullActivitiesList()
  throws NoSuchPlanException
  {
    // GIVEN

    // WHEN
    final CreatedPlan ids = this.planRepository.storePlan(new Plan());

    // THEN
    final var directives = this.planRepository.getPlanForValidation(ids.planId()).activityDirectives;
    assertNotNull(directives);
    assertTrue(directives.isEmpty());
  }

  @Test
  public void testCanDeletePlan() throws NoSuchPlanException {
    // GIVEN
    this.planRepository.storePlan(new Plan());
    final CreatedPlan ids = this.planRepository.storePlan(new Plan());
    this.planRepository.storePlan(new Plan());
    assertEquals(3, this.planRepository.getAllPlans().size());

    // WHEN
    this.planRepository.deletePlan(ids.planId());

    // THEN
    assertEquals(2, this.planRepository.getAllPlans().size());
  }
}
