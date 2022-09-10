package gov.nasa.jpl.aerie.foomissionmodel;

import gov.nasa.jpl.aerie.foomissionmodel.activities.FooActivity;
import gov.nasa.jpl.aerie.foomissionmodel.generated.ActivityTypes;
import gov.nasa.jpl.aerie.foomissionmodel.generated.activities.FooActivityMapper;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinTestContext;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.Map;

import static gov.nasa.jpl.aerie.foomissionmodel.generated.ActivityActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
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
  // The `MerlinTestContext` does not need to be declared as a parameter, but will be injected if declared.
  public FooActivityTest(final MerlinTestContext<ActivityTypes, Mission> ctx) {
    // Model configuration can be provided directly, just as for a normal Java class constructor.
    this.model = new Mission(ctx.registrar(), Instant.EPOCH, new Configuration());

    // Before spawning activities, a model instance must be registered with the test harness.
    ctx.use(model);
  }

  @Test
  public void testActivity() {
    // Within a Merlin test, simulation actions and test assertions can be mixed freely.
    spawn(model, new FooActivity());
    delay(1, Duration.SECOND);
    assertThat(model.simpleData.totalVolume.get()).isCloseTo(15.0, within(1e-9));
    delay(10, Duration.SECOND);
    assertThat(model.simpleData.totalVolume.get()).isCloseTo(147.558135, within(1e-9));
  }

  @Test
  public void testActivityInstantiate() {

    // Assert missing required argument throws exception
    assertThatExceptionOfType(InstantiationException.class).isThrownBy(() ->
    new FooActivityMapper().getInputType().instantiate(Map.of(
        "x", SerializedValue.of(42),
        "y", SerializedValue.of("test")
    )));

    // Assert provided required argument throws no exception
    assertThatNoException().isThrownBy(() ->
        new FooActivityMapper().getInputType().instantiate(Map.of(
           "x", SerializedValue.of(42),
           "y", SerializedValue.of("test"),
           "z", SerializedValue.of(43)
    )));
  }
}
