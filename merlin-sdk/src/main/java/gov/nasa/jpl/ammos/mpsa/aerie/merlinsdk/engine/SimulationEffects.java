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


  public static SimulationContext.SpawnedActivityHandle defer(final Duration duration, final Activity<?> activity) {
     return activeContext.get().defer(duration, activity);
  }

  public static SimulationContext.SpawnedActivityHandle defer(final long quantity, final TimeUnit units, final Activity<?> activity) {
    return defer(Duration.fromQuantity(quantity, units), activity);
  }

  public static SimulationContext.SpawnedActivityHandle deferTo(final Instant instant, final Activity<?> activity) {
    return defer(instant.durationFrom(now()), activity);
  }

  public static SimulationContext.SpawnedActivityHandle spawn(final Activity<?> activity) {
    return defer(0, TimeUnit.MICROSECONDS, activity);
  }

  public static void call(final Activity<?> activity) {
    spawn(activity).await();
  }


  public static void delay(final Duration duration) {
    activeContext.get().delay(duration);
  }

  public static void delay(final long quantity, final TimeUnit units) {
    delay(Duration.fromQuantity(quantity, units));
  }


  public static void waitForChildren() {
    activeContext.get().waitForAllChildren();
  }

  public static Instant now() {
    return activeContext.get().now();
  }
}
