package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

public final class ReplayingTask<Output> implements Task<Unit, Output> {
  private final Scoped<Context> rootContext;
  private final Supplier<Output> task;

  private final ReplayingReactionContext.Memory memory = new ReplayingReactionContext.Memory(new ArrayList<>(), new MutableInt(0));

  public ReplayingTask(final Scoped<Context> rootContext, final Supplier<Output> task) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.task = Objects.requireNonNull(task);
  }

  @Override
  public TaskStatus<Output> step(final Scheduler scheduler, final Unit input) {
    final var handle = new ReplayingTaskHandle();
    final var context = new ReplayingReactionContext(this.rootContext, this.memory, scheduler, handle);

    try (final var restore = this.rootContext.set(context)) {
      final var returnValue = this.task.get();

      // If we get here, the activity has completed normally.
      return TaskStatus.completed(returnValue);
    } catch (final Yield ignored) {
      // If we get here, the activity has suspended.
      return Objects.requireNonNull(handle.status, "Task status is null, but it should have been assigned a value on yield.");
    }
  }

  private final class ReplayingTaskHandle implements TaskHandle {
    public TaskStatus<Output> status = null;

    private Scheduler yield(final TaskStatus<Output> status) {
      this.status = status;
      throw Yield;
    }

    @Override
    public Scheduler delay(final Duration delay) {
      return this.yield(TaskStatus.delayed(delay, ReplayingTask.this));
    }

    @Override
    public Scheduler call(final TaskFactory<Unit, ?> child) {
      return this.yield(TaskStatus.calling(child, ReplayingTask.this));
    }

    @Override
    public Scheduler await(final gov.nasa.jpl.aerie.merlin.protocol.model.Condition condition) {
      return this.yield(TaskStatus.awaiting(condition, ReplayingTask.this));
    }
  }

  // Since this exception is just used to transfer control out of an activity,
  //   we can pre-allocate a single instance as a unique token
  //   to avoid some of the overhead of exceptions
  //   (most notably the call stack snapshotting).
  private static final class Yield extends RuntimeException {}
  private static final Yield Yield = new Yield();
}
