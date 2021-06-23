package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ReplayingTask<$Timeline> implements Task<$Timeline> {
  private final Scoped<Context> rootContext;
  private final Runnable task;

  private final List<ActivityBreadcrumb<$Timeline>> breadcrumbs = new ArrayList<>();

  public ReplayingTask(final Scoped<Context> rootContext, final Runnable task) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.task = Objects.requireNonNull(task);
  }

  @Override
  public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
    this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(scheduler.now()));

    final var handle = new ReplayingTaskHandle<$Timeline>();
    final var context = new ReactionContext<>(this.rootContext, this.breadcrumbs, scheduler, handle);

    try {
      this.rootContext.setWithin(context, this.task::run);

      // If we get here, the activity has completed normally.
      return TaskStatus.completed();
    } catch (final Yield ignored) {
      // If we get here, the activity has suspended.
      return handle.status;
    }
  }

  @Override
  public void reset() {
    this.breadcrumbs.clear();
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
