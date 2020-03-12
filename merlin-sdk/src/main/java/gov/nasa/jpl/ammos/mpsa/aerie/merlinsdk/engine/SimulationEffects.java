package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

public final class SimulationEffects {
  private SimulationEffects() {}

  private static final DynamicCell<SimulationContext> activeContext = new DynamicCell<>();

  public static <Throws extends Throwable>
  void withEffects(final SimulationContext ctx, final DynamicCell.BlockScope<Throws> scope) throws Throws {
    activeContext.setWithin(ctx, scope);
  }

  public static void spawn(final Activity<?> activity) {
     activeContext.get().spawnActivity(activity);
  }

  public static void spawnAndWait(final Activity<?> activity) {
    activeContext.get().callActivity(activity);
  }

  public static void delay(final Duration duration) {
    activeContext.get().delay(duration);
  }

  public static void delay(final long quantity, final TimeUnit units) {
    activeContext.get().delay(quantity, units);
  }

  public static void waitForChildren() {
    activeContext.get().waitForAllChildren();
  }

  public static Instant now() {
    return activeContext.get().now();
  }
}
