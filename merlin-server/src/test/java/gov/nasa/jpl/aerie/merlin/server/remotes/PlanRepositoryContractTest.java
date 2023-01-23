package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.mocks.InMemoryPlanRepository;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository.CreatedPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class PlanRepositoryContractTest {
  protected InMemoryPlanRepository planRepository = null;

  protected abstract void resetRepository();

  @BeforeEach
  public void resetRepositoryBeforeEachTest() {
    this.resetRepository();
  }

  @Test
  public void testCanStorePlan() throws NoSuchPlanException, MissionModelRepository.NoSuchMissionModelException {
    // GIVEN

    // WHEN
    final NewPlan newPlan = new NewPlan();
    newPlan.name = "new-plan";

    final CreatedPlan ids = this.planRepository.createPlan(newPlan);

    // THEN
    final Plan plan = this.planRepository.getPlan(ids.planId());
    assertThat(plan.name).isEqualTo("new-plan");
  }

  @Test
  public void testUnsavedPlanTransactionHasNoEffect()
  throws NoSuchPlanException
  {
    // GIVEN
    final NewPlan newPlan = new NewPlan();
    newPlan.name = "before";
    final CreatedPlan ids = this.planRepository.createPlan(newPlan);

    // WHEN
    this.planRepository
        .updatePlan(ids.planId())
        .setName("after");
        // no .commit()

    // THEN
    final Plan plan = this.planRepository.getPlan(ids.planId());
    assertThat(plan.name).isEqualTo("before");
  }

  @Test
  public void testCreatePlanWithActivity() throws NoSuchPlanException {
    // GIVEN

    // WHEN
    final ActivityDirective activity = new ActivityDirective();
    activity.type = "abc";
    activity.arguments = Map.of("abc", SerializedValue.of(1));

    final NewPlan newPlan = new NewPlan();
    newPlan.name = "new-plan";
    newPlan.activityDirectives = List.of();

    final CreatedPlan ids = this.planRepository.createPlan(newPlan);
    this.planRepository.createActivity(ids.planId(), activity);

    // THEN
    final Plan plan = this.planRepository.getPlan(ids.planId());
    assertThat(plan.name).isEqualTo("new-plan");
    assertThat(plan.activityInstances.values()).containsExactly(activity);
  }

  @Test
  public void testCreatePlanWithNullActivitiesList()
  throws NoSuchPlanException
  {
    // GIVEN

    // WHEN
    final CreatedPlan ids = this.planRepository.createPlan(new NewPlan());

    // THEN
    assertThat(this.planRepository.getPlan(ids.planId()).activityInstances).isNotNull().isEmpty();
  }

  @Test
  public void testCanDeletePlan() throws NoSuchPlanException {
    // GIVEN
    this.planRepository.createPlan(new NewPlan());
    final CreatedPlan ids = this.planRepository.createPlan(new NewPlan());
    this.planRepository.createPlan(new NewPlan());
    assertThat(this.planRepository.getAllPlans()).size().isEqualTo(3);

    // WHEN
    this.planRepository.deletePlan(ids.planId());

    // THEN
    assertThat(this.planRepository.getAllPlans()).size().isEqualTo(2);
  }
}
