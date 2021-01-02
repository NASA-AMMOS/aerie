package gov.nasa.jpl.aerie.services.plan.mocks;

import gov.nasa.jpl.aerie.services.plan.remotes.PlanRepositoryContractTest;

public final class MockPlanRepositoryTest extends PlanRepositoryContractTest {
  @Override
  protected void resetRepository() {
    this.planRepository = new MockPlanRepository();
  }
}
