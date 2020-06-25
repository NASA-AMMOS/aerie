package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier.getRateOf;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier.getVolumeOf;

public final class States {
  private static final DataBinsResource bins = new DataBinsResource(getVolumeOf, getRateOf, event -> ctx.emit(event));

  public static final DataBinResource binA = bins.bin("bin A");

  public static final CumulableResource<String> log = new LogResource(ctx::emit);

  public static void call(final Activity activity) {
    spawn(activity).await();
  }

  public static SpawnHandle spawn(final Activity activity) {
    final var childId = ctx.spawn(activity);
    return () -> ctx.waitForActivity(childId);
  }

  public static SpawnHandle spawnAfter(final Duration delay, final Activity activity) {
    final var childId = ctx.spawnAfter(delay, activity);
    return () -> ctx.waitForActivity(childId);
  }

  public static void waitForChildren() {
    ctx.waitForChildren();
  }

  public static void delay(final long quantity, final TimeUnit unit) {
    ctx.delay(Duration.of(quantity, unit));
  }

  @FunctionalInterface
  public interface SpawnHandle {
    void await();
  }
}
