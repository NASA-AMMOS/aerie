package gov.nasa.jpl.aerie.foomissionmodel;

import gov.nasa.jpl.aerie.foomissionmodel.activities.FooActivity;
import gov.nasa.jpl.aerie.foomissionmodel.generated.ActivityTypes;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinTestContext;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;

import static gov.nasa.jpl.aerie.foomissionmodel.generated.ActivityActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public final class MissionConfigurationTest {
  @TestInstance(Lifecycle.PER_CLASS)
  public @Nested final class Test1 {

    @RegisterExtension
    public static final MerlinExtension<ActivityTypes, Mission> ext = new MerlinExtension<>();

    private final Mission model;

    public Test1(final MerlinTestContext<ActivityTypes, Mission> ctx) {
      this.model = new Mission(ctx.registrar(), Instant.EPOCH, new Configuration());
      ctx.use(model);
    }

    @Test
    public void test() {
      assertThat(model.startingAfterUnixEpoch.get()).isEqualTo(false);
      spawn(model, new FooActivity());
      delay(1, Duration.SECOND);
      assertThat(model.sink.get()).isCloseTo(0.5, within(1e-9));
    }
  }

  @TestInstance(Lifecycle.PER_CLASS)
  public @Nested final class Test2 {

    @RegisterExtension
    public static final MerlinExtension<ActivityTypes, Mission> ext = new MerlinExtension<>();

    private final Mission model;

    public Test2(final MerlinTestContext<ActivityTypes, Mission> ctx) {
      this.model = new Mission(ctx.registrar(), Instant.EPOCH.plusSeconds(1), new Configuration(2.0));
      ctx.use(model);
    }

    @Test
    public void test() {
      assertThat(model.startingAfterUnixEpoch.get()).isEqualTo(true);
      spawn(model, new FooActivity());
      delay(1, Duration.SECOND);
      assertThat(model.sink.get()).isCloseTo(2.0, within(1e-9));
    }
  }
}
