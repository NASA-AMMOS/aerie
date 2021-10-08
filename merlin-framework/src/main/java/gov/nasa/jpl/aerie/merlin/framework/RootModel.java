package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.types.Phantom;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The top-level mission model managed by the Merlin framework.
 */
public record RootModel<$Schema, Model>(Model model, ExecutorService executor) implements AutoCloseable {
  public RootModel {}
  public static ExecutorService makeExecutorService() {
    return Executors.newCachedThreadPool($ -> {
      final var t = new Thread($);
      // TODO: Make threads non-daemons once the model can be closed via the `AdaptationFactory` interface.
      //  We're marking these as daemons right now solely to ensure that the JVM shuts down cleanly in lieu of
      //  proper model lifecycle management.
      //  In fact, daemon threads can mask bad memory leaks: a hanging thread is almost indistinguishable
      //  from a dead thread.
      t.setDaemon(true);
      return t;
    });
  }

  @Override
  public void close() {
    this.executor.shutdownNow();
  }

  public static <$Schema, Model>
  RootModel<$Schema, Model> fromPhantom(final Phantom<$Schema, RootModel<?, Model>> wrapper) {
    // SAFETY: By convention, a Phantom<$, F<?>> represents an F<$> for any F.
    @SuppressWarnings("unchecked")
    final var model = (RootModel<$Schema, Model>) wrapper.value();
    return model;
  }

  public Phantom<$Schema, RootModel<?, Model>> toPhantom() {
    return new Phantom<>(this);
  }
}
