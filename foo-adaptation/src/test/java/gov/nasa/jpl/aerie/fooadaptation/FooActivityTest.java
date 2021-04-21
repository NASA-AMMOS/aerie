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

// The `@ExtendWith` annotation injects the given extension into JUnit's testing apparatus.
// Our `MerlinExtension` hooks test class construction and test method execution,
//   executing each with the appropriate simulation context.
@ExtendWith(MerlinExtension.class)
public final class FooActivityTest {
  private final Mission model;

  // Initializers and the test class constructor are executed in an "initialization" Merlin context.
  // This means that models can be created (and cell storage allocated, and daemons spawned),
  //   but simulation control actions like `waitFor`, `delay`, and `emit` cannot be performed.
  // The `Registrar` does not need to be declared as a parameter, but will be injected if declared.
  public FooActivityTest(final Registrar registrar) {
    // Model configuration can be provided directly, just as for a normal Java class constructor.
    this.model = new Mission(registrar, new Configuration());

    // Activities must be registered explicitly in order to be used in testing.
    // The generated `ActivityTypes` helper class loads all declared activities,
    //   but focused subsystem tests might register only the activities under test.
    ActivityTypes.register(registrar, this.model);
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
