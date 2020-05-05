package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import java.util.function.Consumer;
import java.util.function.Function;

public final class SimulationEffects {
  private SimulationEffects() {}

  private static final DynamicCell<SimulationContext> activeContext = new DynamicCell<>();
  private static final DynamicCell<Function<Runnable, Runnable>> activeDecorator = new DynamicCell<>();

  public static Consumer<SimulationContext> withEffects(final Function<Runnable, Runnable> decorator, final Runnable scope) {
    return (ctx) ->
        activeContext.setWithin(ctx, () ->
            activeDecorator.setWithin(decorator, () ->
                decorator.apply(scope).run()));
  }

  public static Consumer<SimulationContext> withEffects(final Runnable scope) {
    return withEffects(task -> task, scope);
  }

  /**
   * Spawn a new activity as a child of the currently-running activity after a given span of time.
   */
  public static SimulationContext.SpawnedActivityHandle defer(final Duration duration, final Activity activity) {
    return activeContext.get().defer(duration, withEffects(activeDecorator.get(), activity::modelEffects));
  }

  /**
   * Spawn a parallel branch of the currently-running activity.
   */
  public static void spawn(final Runnable scope) {
    activeContext.get().defer(Duration.ZERO, withEffects(activeDecorator.get(), scope));
  }

  /**
   * Delay the currently-running activity for the given duration.
   */
  public static void delay(final Duration duration) {
    activeContext.get().delay(duration);
  }

  /**
   * Delay the currently-running activity until all of its existing children have completed.
   */
  public static void waitForChildren() {
    activeContext.get().waitForAllChildren();
  }

  /**
   * Get the current simulation time.
   */
  public static Instant now() {
    return activeContext.get().now();
  }


  /**
   * Spawn a new activity as a child of the currently-running activity after a given span of time.
   */
  public static SimulationContext.SpawnedActivityHandle defer(final long quantity, final TimeUnit units, final Activity activity) {
    return defer(Duration.of(quantity, units), activity);
  }

  /**
   * Spawn a new activity as a child of the currently-running activity at a given point in time.
   */
  public static SimulationContext.SpawnedActivityHandle deferTo(final Instant instant, final Activity activity) {
    return defer(now().durationTo(instant), activity);
  }

  /**
   * Spawn a new activity as a child of the currently-running activity.
   */
  public static SimulationContext.SpawnedActivityHandle spawn(final Activity activity) {
    return defer(Duration.ZERO, activity);
  }

  /**
   * Spawn a new activity as a child of the currently-running activity, and wait until it completes.
   * @param activity
   */
  public static void call(final Activity activity) {
    spawn(activity).await();
  }

  /**
   * Delay the currently-running activity for the given duration.
   */
  public static void delay(final long quantity, final TimeUnit units) {
    delay(Duration.of(quantity, units));
  }
}
