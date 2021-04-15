package gov.nasa.jpl.aerie.fooadaptation;

import static org.junit.Assert.assertEquals;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

import gov.nasa.jpl.aerie.fooadaptation.models.SimpleData;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.framework.ModelTestFramework;
import gov.nasa.jpl.aerie.time.Duration;
import org.junit.Test;

public class SimpleDataTest {
  @Test
  public void testTotalVolume() {
    ModelTestFramework.test(
      r -> new SimpleData(),
      (model) -> {
        model.a.activate();
        model.b.activate();
        delay(Duration.SECOND);
        assertEquals(15.0, model.totalVolume.get(), 1e-9);
      });
  }
}
