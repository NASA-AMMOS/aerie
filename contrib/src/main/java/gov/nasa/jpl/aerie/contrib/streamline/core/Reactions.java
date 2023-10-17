package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.dynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.when;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.replaying;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

public final class Reactions {
  private Reactions() {}

  public static void whenever(Resource<Discrete<Boolean>> conditionResource, Runnable action) {
    whenever(when(conditionResource), action);
  }

  public static void whenever(Condition condition, Runnable action) {
    whenever(() -> condition, action);
  }

  public static void whenever(Supplier<Condition> trigger, Runnable action) {
    final Condition condition = trigger.get();
    // Use replaying tasks to avoid threading overhead.
    spawn(replaying(() -> {
      waitUntil(condition);
      action.run();
      // Trampoline off this task to avoid replaying.
      whenever(trigger, action);
    }));
  }

  // Special case for dynamicsChange condition, since it's non-obvious that this needs to be run in lambda form
  public static <D extends Dynamics<?, D>> void wheneverDynamicsChange(Resource<D> resource, Consumer<ErrorCatching<Expiring<D>>> reaction) {
    whenever(() -> dynamicsChange(resource), () -> reaction.accept(resource.getDynamics()));
  }

  public static void every(Duration period, Runnable action) {
    every(() -> period, action);
  }

  public static void every(Supplier<Duration> periodSupplier, Runnable action) {
    spawn(replaying(() -> {
      delay(periodSupplier.get());
      action.run();
      every(periodSupplier, action);
    }));
  }
}
