package gov.nasa.jpl.aerie.fooadaptation;

import gov.nasa.jpl.aerie.fooadaptation.activities.FooActivity;
import gov.nasa.jpl.aerie.fooadaptation.generated.ActivityTypes;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.fooadaptation.generated.ActivityActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MerlinExtension.class)
public final class MissionConfigurationTest {
  public @Nested final class Test1 {
    private final Mission model;

    public Test1(final Registrar registrar) {
      this.model = new Mission(registrar, new Configuration());
      ActivityTypes.register(registrar, this.model);
    }

    @Test
    public void test() {
      spawn(new FooActivity());
      delay(1, Duration.SECOND);
      assertThat(model.sink.get()).isCloseTo(0.5, within(1e-9));
    }
  }

  public @Nested final class Test2 {
    private final Mission model;

    public Test2(final Registrar registrar) {
      this.model = new Mission(registrar, new Configuration(2.0));
      ActivityTypes.register(registrar, this.model);
    }

    @Test
    public void test() {
      spawn(new FooActivity());
      delay(1, Duration.SECOND);
      assertThat(model.sink.get()).isCloseTo(2.0, within(1e-9));
    }
  }
}
