package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public final class ReplayingTask implements Task {
  private final ExecutorService executor;
  private final Scoped<Context> rootContext;
  private final Runnable task;

  private final ReplayingReactionContext.Memory memory = new ReplayingReactionContext.Memory(new ArrayList<>(), new MutableInt(0));

  public ReplayingTask(final ExecutorService executor, final Scoped<Context> rootContext, final Runnable task) {
    this.executor = Objects.requireNonNull(executor);
    this.rootContext = Objects.requireNonNull(rootContext);
    this.task = Objects.requireNonNull(task);
  }

  @Override
  public TaskStatus step(final Scheduler scheduler) {
    final var handle = new ReplayingTaskHandle();
    final var context = new ReplayingReactionContext(this.executor, this.rootContext, this.memory, scheduler, handle);

    try (final var restore = this.rootContext.set(context)){
      this.task.run();

      // If we get here, the activity has completed normally.
      return TaskStatus.completed();
    } catch (final Yield ignored) {
      // If we get here, the activity has suspended.
      return handle.status;
    }
  }

  @Override
  public void reset() {
    this.memory.clear();
  }

  private static final class ReplayingTaskHandle implements TaskHandle {
    public TaskStatus status = TaskStatus.completed();

    @Override
    public Scheduler yield(final TaskStatus status) {
      this.status = status;
      throw Yield;
    }
  }

  // Since this exception is just used to transfer control out of an activity,
  //   we can pre-allocate a single instance as a unique token
  //   to avoid some of the overhead of exceptions
  //   (most notably the call stack snapshotting).
  private static final class Yield extends RuntimeException {}
  private static final Yield Yield = new Yield();
}
