package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository.ActivityTransaction;
import static gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository.PlanTransaction;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class PlanRepositoryContractTest {
  protected PlanRepository planRepository = null;

  protected abstract void resetRepository();

  @BeforeEach
  public void resetRepositoryBeforeEachTest() {
    this.resetRepository();
  }

  @Test
  public void testStoredPlanIsIndependent() {
    // GIVEN
    final PlanTransaction newTransaction = this.planRepository.newPlan();
    newTransaction.setName("before");
    final String planId = newTransaction.save();

    // WHEN
    newTransaction.setName("after");

    // THEN
    final PlanTransaction getTransaction = this.planRepository.getPlan(planId).get();
    assertThat(getTransaction.get().name).isEqualTo("before");
  }

  @Test
  public void testUnsavedPlanTransactionHasNoEffect() {
    // GIVEN

    // WHEN
    final PlanTransaction newTransaction = this.planRepository.newPlan();
    newTransaction.setName("test");

    // THEN
    assertThat(this.planRepository.getAllPlans().collect(Collectors.toList())).isEmpty();
  }

  @Test
  public void testUnsavedActivityTransactionHasNoEffect() {
    // GIVEN
    final PlanTransaction planTransaction = this.planRepository.newPlan();

    // WHEN
    final ActivityTransaction activityTransaction = planTransaction.newActivity();
    activityTransaction.setType("abc");

    // THEN
    final Plan plan = planTransaction.get();
    assertThat(plan.activityInstances).isEmpty();
  }

  @Test
  public void testCreatePlanWithActivity() {
    // GIVEN

    // WHEN
    final PlanTransaction planTransaction = this.planRepository.newPlan();
    final String activityId = planTransaction.newActivity()
        .setType("abc")
        .save();
    final String planId = planTransaction.save();

    // THEN
    ActivityInstance activity = this.planRepository
        .getPlan(planId).get()
        .getActivity(activityId).get()
        .get();
    assertThat(activity.type).isEqualTo("abc");
  }

  @Test
  public void testCanDeleteAllPlans() {
    // GIVEN
    this.planRepository.newPlan().save();
    this.planRepository.newPlan().save();
    this.planRepository.newPlan().save();
    assertThat(this.planRepository.getAllPlans()).size().isEqualTo(3);

    // WHEN
    this.planRepository.getAllPlans().forEach(PlanTransaction::delete);

    // THEN
    assertThat(this.planRepository.getAllPlans()).isEmpty();
  }

  @Test
  public void testCanDeleteAllActivityInstances() {
    // GIVEN
    final PlanTransaction transaction = this.planRepository.newPlan();
    transaction.newActivity().save();
    transaction.newActivity().save();
    transaction.newActivity().save();
    final String planId = transaction.save();

    assertThat(this.planRepository.getPlan(planId).get().getAllActivities()).size().isEqualTo(3);

    // WHEN
    final PlanTransaction deleteTransaction = this.planRepository.getPlan(planId).get();
    deleteTransaction.getAllActivities().forEach(ActivityTransaction::delete);
    deleteTransaction.save();

    // THEN
    assertThat(this.planRepository.getPlan(planId).get().getAllActivities()).isEmpty();
  }
}
