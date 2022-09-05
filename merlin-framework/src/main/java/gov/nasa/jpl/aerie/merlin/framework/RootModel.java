package gov.nasa.jpl.aerie.merlin.framework;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The top-level mission model managed by the Merlin framework.
 */
public record RootModel<Registry, Model>(Model model, ExecutorService executor) implements AutoCloseable {
  public static ExecutorService makeExecutorService() {
    return Executors.newCachedThreadPool($ -> {
      final var t = new Thread($);
      // TODO: Make threads non-daemons once the model can be closed via the `MissionModelFactory` interface.
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
}
