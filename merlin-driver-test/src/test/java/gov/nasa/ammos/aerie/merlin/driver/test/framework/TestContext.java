package gov.nasa.ammos.aerie.merlin.driver.test.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;

import java.util.Objects;

/**
 * Responsible for enabling static methods to look up the simulator's scheduler and call methods on it
 */
public class TestContext {
  private static Context currentContext = null;

  public record Context(TestRegistrar.CellMap cells, Scheduler scheduler, ThreadedTask<?> threadedTask) {}

  public static Context get() {
    return currentContext;
  }

  public static void set(Context context) {
    Objects.requireNonNull(context, "Use clear() instead");
    currentContext = context;
  }

  public static void clear() {
    currentContext = null;
  }
}
