package gov.nasa.jpl.aerie.fooadaptation;

import gov.nasa.jpl.aerie.fooadaptation.activities.FooActivity;
import gov.nasa.jpl.aerie.fooadaptation.generated.ActivityTypes;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.fooadaptation.generated.ActivityActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MerlinExtension.class)
public final class FooActivityTest {
  private final Mission model;

  public FooActivityTest(final Registrar registrar) {
    this.model = new Mission(registrar, new Configuration());
    ActivityTypes.register(registrar, this.model);
  }

  @Test
  public void testActivity() {
    spawn(new FooActivity());
    delay(1, Duration.SECOND);
    assertThat(model.simpleData.totalVolume.get()).isCloseTo(15.0, within(1e-9));
    delay(10, Duration.SECOND);
    assertThat(model.simpleData.totalVolume.get()).isCloseTo(147.558135, within(1e-9));
  }
}
