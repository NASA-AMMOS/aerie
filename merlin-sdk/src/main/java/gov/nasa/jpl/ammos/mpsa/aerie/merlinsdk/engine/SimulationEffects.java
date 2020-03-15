package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import java.util.function.Consumer;

public final class SimulationEffects {
  private SimulationEffects() {}

  private static final DynamicCell<SimulationContext> activeContext = new DynamicCell<>();

  public static <Throws extends Throwable>
  void withEffects(final SimulationContext ctx, final DynamicCell.BlockScope<Throws> scope) throws Throws {
    activeContext.setWithin(ctx, scope);
  }


  public static SimulationContext.SpawnedActivityHandle defer(final Duration duration, final Activity<?> activity) {
    final Consumer<StateContainer> modelEffects = ((Activity<StateContainer>)activity)::modelEffects;
    return activeContext.get().defer(duration, (ctx) -> {
      withEffects(ctx, () -> modelEffects.accept(ctx.getActiveStateContainer()));
    });
  }

  public static SimulationContext.SpawnedActivityHandle defer(final long quantity, final TimeUnit units, final Activity<?> activity) {
    return defer(Duration.of(quantity, units), activity);
  }

  public static SimulationContext.SpawnedActivityHandle deferTo(final Instant instant, final Activity<?> activity) {
    return defer(now().durationTo(instant), activity);
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
    delay(Duration.of(quantity, units));
  }


  public static void waitForChildren() {
    activeContext.get().waitForAllChildren();
  }

  public static Instant now() {
    return activeContext.get().now();
  }
}
