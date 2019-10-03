package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepositoryContractTest;

public final class MockPlanRepositoryTest extends PlanRepositoryContractTest {
  @Override
  protected void resetRepository() {
    this.planRepository = new MockPlanRepository();
  }
}
