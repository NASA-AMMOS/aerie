package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import org.junit.jupiter.api.BeforeEach;

public abstract class AdaptationServiceContractTest {
  protected AdaptationService adaptationService = null;

  protected abstract void resetService();

  @BeforeEach
  public void resetServiceBeforeEachTest() {
    this.resetService();
  }
}
