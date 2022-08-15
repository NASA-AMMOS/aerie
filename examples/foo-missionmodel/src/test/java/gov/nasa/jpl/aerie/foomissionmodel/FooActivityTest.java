package gov.nasa.jpl.aerie.foomissionmodel;

import java.time.Instant;
import gov.nasa.jpl.aerie.foomissionmodel.activities.FooActivity;
import gov.nasa.jpl.aerie.foomissionmodel.generated.ActivityTypes;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinTestContext;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.RegisterExtension;

import static gov.nasa.jpl.aerie.foomissionmodel.generated.ActivityActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@TestInstance(Lifecycle.PER_CLASS)
public final class FooActivityTest {

  // The `@RegisterExtension` annotation programmatically injects the given extension into JUnit's testing apparatus.
  // Our `MerlinExtension` hooks test class construction and test method execution,
  //   executing each with the appropriate simulation context.
  @RegisterExtension
  public static final MerlinExtension<ActivityTypes, Mission> ext = new MerlinExtension<>();

  private final Mission model;

  // Initializers and the test class constructor are executed in an "initialization" Merlin context.
  // This means that models can be created (and cell storage allocated, and daemons spawned),
  //   but simulation control actions like `waitFor`, `delay`, and `emit` cannot be performed.
  // The `Registrar` does not need to be declared as a parameter, but will be injected if declared.
  public FooActivityTest(final MerlinTestContext<ActivityTypes, Mission> ctx) {
    // Model configuration can be provided directly, just as for a normal Java class constructor.
    this.model = new Mission(ctx.registrar(), Instant.EPOCH, new Configuration());

    // Activities must be registered explicitly in order to be used in testing.
    // The generated `ActivityTypes` helper class loads all declared activities,
    //   but focused subsystem tests might register only the activities under test.
    ctx.use(model, ActivityTypes::register);
  }


  @Test
  public void testActivity() {
    // Within a Merlin test, simulation actions and test assertions can be mixed freely.
    spawn(new FooActivity());
    delay(1, Duration.SECOND);
    assertThat(model.simpleData.totalVolume.get()).isCloseTo(15.0, within(1e-9));
    delay(10, Duration.SECOND);
    assertThat(model.simpleData.totalVolume.get()).isCloseTo(147.558135, within(1e-9));
  }
}
