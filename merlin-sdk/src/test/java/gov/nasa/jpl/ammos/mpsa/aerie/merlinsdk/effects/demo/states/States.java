package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities.ReactionContext.activeContext;

public final class States {
  private static final DataBinsResource bins =
      new DataBinsResource(
          () -> activeContext.get().as(Querier::getDataModel),
          event -> activeContext.get().emit(event));

  public static final DataBinResource binA = bins.bin("bin A");

  public static final CumulableResource<String> log =
      new LogResource(event -> activeContext.get().emit(event));

  public static void call(final String activity) {
    spawn(activity).await();
  }

  public static SpawnHandle spawn(final String activity) {
    final var childId = activeContext.get().spawn(activity);
    return () -> activeContext.get().waitForActivity(childId);
  }

  public static void waitForChildren() {
    activeContext.get().waitForChildren();
  }

  @FunctionalInterface
  public interface SpawnHandle {
    void await();
  }

  public static void delay(final long quantity, final TimeUnit unit) {
    activeContext.get().delay(Duration.of(quantity, unit));
  }
}
