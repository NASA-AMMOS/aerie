package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

public final class ThreadedTask<$Schema, $Timeline extends $Schema>
    implements Task<$Timeline>
{
  private final DynamicCell<Context<$Schema>> rootContext;
  private final ThreadedTaskHandle task;
  private final List<ActivityBreadcrumb<$Timeline>> breadcrumbs = new ArrayList<>();

  public ThreadedTask(final DynamicCell<Context<$Schema>> rootContext, final Runnable task) {
    this.rootContext = rootContext;
    this.task = new ThreadedTaskHandle(task);
  }

  @Override
  public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
    this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(scheduler.now()));

    final var context = new ReactionContext<$Schema, $Timeline>(
        this.breadcrumbs.size() - 1,
        this.breadcrumbs,
        scheduler,
        this.task);

    this.rootContext.setWithin(context, this.task::resumeTask);

    return context.getStatus();
  }

  private static final class ThreadedTaskHandle implements TaskHandle {
    private enum Token { TOKEN }

    private final Runnable task;
    private final Thread thread;

    private final ArrayBlockingQueue<Token> hostToTask = new ArrayBlockingQueue<>(1);
    private final ArrayBlockingQueue<Token> taskToHost = new ArrayBlockingQueue<>(1);
    private boolean done = false;

    public ThreadedTaskHandle(final Runnable task) {
      this.task = Objects.requireNonNull(task);
      this.thread = new Thread(this::run);

      this.thread.setDaemon(true);
    }

    private void run() {
      try {
        this.hostToTask.take();

        this.task.run();
        this.done = true;

        this.taskToHost.put(Token.TOKEN);
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }
    }

    public void resumeTask() {
      try {
        if (!this.thread.isAlive()) this.thread.start();

        this.hostToTask.put(Token.TOKEN);
        this.taskToHost.take();

        if (this.done) this.thread.join();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin host unexpectedly interrupted", ex);
      }
    }

    public void yieldTask() {
      try {
        this.taskToHost.put(Token.TOKEN);
        this.hostToTask.take();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }
    }
  }
}
