package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

public final class ThreadedTask<$Schema, $Timeline extends $Schema>
    implements Task<$Timeline>
{
  private final Thread thread;
  private final List<ActivityBreadcrumb<$Timeline>> breadcrumbs = new ArrayList<>();

  private final ArrayBlockingQueue<Scheduler<$Timeline>> hostToTask = new ArrayBlockingQueue<>(1);
  private final ArrayBlockingQueue<TaskStatus<$Timeline>> taskToHost = new ArrayBlockingQueue<>(1);
  private boolean done = false;

  public ThreadedTask(final Scoped<Context<$Schema>> rootContext, final Runnable task) {
    Objects.requireNonNull(rootContext);
    Objects.requireNonNull(task);

    final var handle = new ThreadedTaskHandle();

    this.thread = new Thread(() -> {
      try {
        final var scheduler = this.hostToTask.take();
        this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(scheduler.now()));

        final var context = new ReactionContext<$Schema, $Timeline>(this.breadcrumbs, scheduler, handle);
        rootContext.setWithin(context, task::run);

        this.done = true;
        this.taskToHost.put(TaskStatus.completed());
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }
    });

    this.thread.setDaemon(true);
  }

  @Override
  public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
    try {
      if (!this.thread.isAlive()) this.thread.start();

      this.hostToTask.put(scheduler);
      final var status = this.taskToHost.take();

      if (this.done) this.thread.join();

      return status;
    } catch (final InterruptedException ex) {
      throw new Error("Merlin host unexpectedly interrupted", ex);
    }
  }

  private final class ThreadedTaskHandle implements TaskHandle<$Timeline> {
    @Override
    public Scheduler<$Timeline> yield(final TaskStatus<$Timeline> status) {
      try {
        ThreadedTask.this.taskToHost.put(status);
        return ThreadedTask.this.hostToTask.take();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }
    }
  }
}
