package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationServiceContractTest;

public final class MockAdaptationServiceTest extends AdaptationServiceContractTest {
  @Override
  protected void resetService() {
    this.adaptationService = new MockAdaptationService();
  }
}
