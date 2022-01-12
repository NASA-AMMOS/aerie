package gov.nasa.jpl.aerie.configwithdefaults;

import gov.nasa.jpl.aerie.contrib.models.Accumulator;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

public final class Mission {

  public final Accumulator data = new Accumulator();

  private final Registrar cachedRegistrar; // Normally bad practice, only stored to demonstrate built/unbuilt check

  public Mission(final Registrar registrar, final Configuration config) {
    this.cachedRegistrar = registrar;

    spawn(this::test);

    // Assert mission model is unbuilt
    if (registrar.isInitializationComplete()) {
      throw new AssertionError("Registrar should not report initialization as complete");
    }

    registrar.real("/data", this.data);
    registrar.real("/data/rate", this.data.rate);

    spawn(() -> { // Register a never-ending daemon task
      while (true) {
        ModelActions.delay(Duration.SECOND);
      }
    });
  }

  public void test() {
    this.data.rate.add(42.0);

    // Assert mission model is built
    if (!cachedRegistrar.isInitializationComplete())
      throw new AssertionError("Registrar should report initialization as complete");
  }
}
