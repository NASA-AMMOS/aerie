package gov.nasa.jpl.aerie.services.plan.remotes;

import org.junit.Before;

public abstract class AdaptationServiceContractTest {
  protected AdaptationService adaptationService = null;

  protected abstract void resetService();

  @Before
  public void resetServiceBeforeEachTest() {
    this.resetService();
  }
}
