package gov.nasa.ammos.aerie.merlin.driver.test.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Responsible for enabling static methods to look up the simulator's scheduler and call methods on it
 */
public class TestContext {
  private static Context currentContext = null;

  public record Context(TestRegistrar.CellMap cells, Scheduler scheduler, ThreadedTask<?> threadedTask) {}

  public static Context get() {
    return currentContext;
  }

  public static <T> T set(Context context, Supplier<T> supplier) {
    Objects.requireNonNull(context);
    currentContext = context;
    try {
      return supplier.get();
    } finally {
      currentContext = null;
    }
  }
}
