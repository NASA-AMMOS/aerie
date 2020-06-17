package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

public final class States {
  public static final DynamicCell<Pair<ReactionContext<?, String, Event>, Querier.InnerQuerier<?>>> activeContext = DynamicCell.create();

  private static final DataBinsResource bins =
      new DataBinsResource(
          () -> activeContext.get().getRight().getDataModel(),
          event -> activeContext.get().getLeft().emit(event));

  public static final DataBinResource binA = bins.bin("bin A");

  public static final CumulableResource<String> log =
      new LogResource(event -> activeContext.get().getLeft().emit(event));


  public static void call(final String activity) {
    spawn(activity).await();
  }

  public static SpawnHandle spawn(final String activity) {
    final var childId = activeContext.get().getLeft().spawn(activity);
    return () -> activeContext.get().getLeft().waitForActivity(childId);
  }

  public static void waitForChildren() {
    activeContext.get().getLeft().waitForChildren();
  }

  public static void delay(final long quantity, final TimeUnit unit) {
    activeContext.get().getLeft().delay(Duration.of(quantity, unit));
  }

  @FunctionalInterface
  public interface SpawnHandle {
    void await();
  }
}
