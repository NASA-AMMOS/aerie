package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.Objects;

public final class ReplayingTask<$Timeline> implements Task<$Timeline> {
  private final Scoped<Context> rootContext;
  private final Runnable task;

  private final ReactionContext.Memory memory = new ReactionContext.Memory(new ArrayList<>(), new MutableInt(0));

  public ReplayingTask(final Scoped<Context> rootContext, final Runnable task) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.task = Objects.requireNonNull(task);
  }

  @Override
  public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
    final var handle = new ReplayingTaskHandle<$Timeline>();
    final var context = new ReactionContext<>(this.rootContext, this.memory, scheduler, handle);

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

  private static final class ReplayingTaskHandle<$Timeline> implements TaskHandle<$Timeline> {
    public TaskStatus<$Timeline> status = TaskStatus.completed();

    @Override
    public Scheduler<$Timeline> yield(final TaskStatus<$Timeline> status) {
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
