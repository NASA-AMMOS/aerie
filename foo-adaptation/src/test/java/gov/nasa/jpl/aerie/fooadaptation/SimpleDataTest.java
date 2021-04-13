package gov.nasa.jpl.aerie.fooadaptation;

import gov.nasa.jpl.aerie.fooadaptation.models.SimpleData;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MerlinExtension.class)
public final class SimpleDataTest {
  private final SimpleData model = new SimpleData();

  @Test
  public void testTotalVolume() {
    model.a.activate();
    model.b.activate();
    delay(Duration.SECOND);

    assertThat(model.totalVolume.get()).isCloseTo(15.0, within(1e-9));
  }
}
