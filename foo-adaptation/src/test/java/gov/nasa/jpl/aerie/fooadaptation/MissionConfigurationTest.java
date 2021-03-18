package gov.nasa.jpl.aerie.fooadaptation;

import static gov.nasa.jpl.aerie.fooadaptation.generated.ActivityActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static org.junit.Assert.*;

import gov.nasa.jpl.aerie.fooadaptation.activities.FooActivity;
import gov.nasa.jpl.aerie.fooadaptation.generated.GeneratedAdaptationFactory;
import gov.nasa.jpl.aerie.fooadaptation.mappers.ConfigurationMappers;
import gov.nasa.jpl.aerie.merlin.framework.ModelTestFramework;
import gov.nasa.jpl.aerie.time.Duration;
import org.junit.Test;

public class MissionConfigurationTest {

  @Test
  public void testConfiguration1() {

    final var config = new Configuration();

    ModelTestFramework.test(
      new GeneratedAdaptationFactory(),
      ConfigurationMappers.configuration().serializeValue(config),
      registrar -> new Mission(registrar, config),
      model -> {
        spawn(new FooActivity());
        delay(1, Duration.SECOND);
        assertEquals(0.5, model.sink.get(), 1e-9);
      });
  }

  @Test
  public void testConfiguration2() {

    final var config = new Configuration(2.0);

    ModelTestFramework.test(
      new GeneratedAdaptationFactory(),
      ConfigurationMappers.configuration().serializeValue(config),
      registrar -> new Mission(registrar, config),
      model -> {
        spawn(new FooActivity());
        delay(1, Duration.SECOND);
        assertEquals(2.0, model.sink.get(), 1e-9);
      });
  }
}
