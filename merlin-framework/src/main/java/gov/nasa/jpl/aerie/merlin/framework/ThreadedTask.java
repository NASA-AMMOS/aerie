package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

public final class ThreadedTask<$Timeline> implements Task<$Timeline> {
  private final Scoped<Context> rootContext;
  private final Runnable task;

  private final ArrayBlockingQueue<TaskRequest<$Timeline>> hostToTask = new ArrayBlockingQueue<>(1);
  private final ArrayBlockingQueue<TaskResponse<$Timeline>> taskToHost = new ArrayBlockingQueue<>(1);

  private Thread thread = null;
  private boolean isTerminated = false;
  private final List<ActivityBreadcrumb<$Timeline>> breadcrumbs = new ArrayList<>();

  public ThreadedTask(final Scoped<Context> rootContext, final Runnable task) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.task = Objects.requireNonNull(task);
  }

  @Override
  public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
    if (this.isTerminated) return TaskStatus.completed();

    try {
      if (this.thread == null) {
        this.thread = makeThread();
        this.thread.start();
      }

      this.hostToTask.put(new TaskRequest.Resume<>(scheduler));
      final var response = this.taskToHost.take();

      if (response instanceof TaskResponse.Success) {
        final var status = ((TaskResponse.Success<$Timeline>) response).status;

        status.match(new TaskStatus.Visitor<$Timeline, Void>() {
          @Override
          public Void completed() {
            ThreadedTask.this.isTerminated = true;
            return null;
          }

          @Override
          public Void delayed(final Duration delay) {
            return null;
          }

          @Override
          public Void awaiting(final String activityId) {
            return null;
          }

          @Override
          public Void awaiting(final Condition<? super $Timeline> condition) {
            return null;
          }
        });

        return status;
      } else if (response instanceof TaskResponse.Failure) {
        this.thread = null;
        this.isTerminated = true;

        // TODO: Propagate task errors better.
        throw new TaskFailureException(((TaskResponse.Failure<$Timeline>) response).failure);
      } else {
        throw new Error(String.format(
            "Unexpected variant of %s: %s",
            TaskResponse.class.getCanonicalName(),
            response.getClass().getCanonicalName()));
      }
    } catch (final InterruptedException ex) {
      throw new Error("Merlin host unexpectedly interrupted", ex);
    }
  }

  @Override
  public void reset() {
    if (this.thread != null) {
      try {
        this.hostToTask.put(new TaskRequest.Abort<>());
        final var ignored = this.taskToHost.take();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin host unexpectedly interrupted", ex);
      }
    }

    this.thread = null;
    this.isTerminated = false;
    this.breadcrumbs.clear();
  }

  private Thread makeThread() {
    final var handle = new ThreadedTaskHandle();

    final var thread = new Thread(() -> {
      final TaskRequest<$Timeline> request;
      try {
        request = ThreadedTask.this.hostToTask.take();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }

      TaskResponse<$Timeline> response;
      try {
        response = handle.run(request);
      } catch (final Throwable ex) {
        response = new TaskResponse.Failure<>(ex);
      }

      try {
        ThreadedTask.this.taskToHost.put(response);
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }
    });

    return thread;
  }

  private final class ThreadedTaskHandle implements TaskHandle<$Timeline> {
    private boolean isAborting = false;

    public TaskResponse<$Timeline> run(final TaskRequest<$Timeline> request) {
      if (request instanceof TaskRequest.Resume) {
        final var scheduler = ((TaskRequest.Resume<$Timeline>) request).scheduler;

        ThreadedTask.this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(scheduler.now()));
        final var context = new ReactionContext<>(
            ThreadedTask.this.rootContext,
            ThreadedTask.this.breadcrumbs,
            scheduler,
            this);

        try {
          ThreadedTask.this.rootContext.setWithin(context, ThreadedTask.this.task::run);
          return new TaskResponse.Success<>(TaskStatus.completed());
        } catch (final TaskAbort ex) {
          return new TaskResponse.Success<>(TaskStatus.completed());
        } catch (final Throwable ex) {
          return new TaskResponse.Failure<>(ex);
        }
      } else if (request instanceof TaskRequest.Abort) {
        return new TaskResponse.Success<>(TaskStatus.completed());
      } else {
        throw new Error(String.format(
            "Unexpected variant of %s: %s",
            TaskRequest.class.getCanonicalName(),
            request.getClass().getCanonicalName()));
      }
    }

    @Override
    public Scheduler<$Timeline> yield(final TaskStatus<$Timeline> status) {
      // If we're in the middle of aborting, just keep trying to bail out.
      if (this.isAborting) throw TaskAbort;

      // Get the next request from the driver.
      final TaskRequest<$Timeline> request;
      try {
        ThreadedTask.this.taskToHost.put(new TaskResponse.Success<>(status));
        request = ThreadedTask.this.hostToTask.take();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }

      if (request instanceof TaskRequest.Resume) {
        // We've been told to continue executing.
        return ((TaskRequest.Resume<$Timeline>) request).scheduler;
      } else if (request instanceof TaskRequest.Abort) {
        // We've been told to bail out and release this thread ASAP.
        //
        // We'll throw an exception to get as far up and out of the task as we can.
        // If the task intercepts this exception (via `catch` or `finally`),
        // it may attempt to perform more simulation effects.
        // We'll just keep throwing whenever `yield()` is called.
        //
        // The task might also busy-loop without ever passing control back.
        // This would be poor behavior even for an active task, so there's not much
        // we can do except build the driver thread to be resilient against ill-behaved tasks.
        //
        // TODO: Don't let the ReactionContext interact directly with the scheduler.
        //   We should intercept calls to the scheduler so that they, too, cause a `TaskAbort`.
        //   As it stands, they will cause a `NullPointerException`, since `ReactionContext`
        //   sets its `scheduler` field to null before yielding. That's fine -- it keeps us bailing --
        //   but it's not great to have this interaction logic spread out.
        this.isAborting = true;
        throw TaskAbort;
      } else {
        throw new Error(String.format(
            "Unexpected variant of %s: %s",
            TaskRequest.class.getCanonicalName(),
            request.getClass().getCanonicalName()));
      }
    }
  }

  /*sealed*/ interface TaskRequest<$Timeline> {
    final class Resume<$Timeline> implements TaskRequest<$Timeline> {
      public final Scheduler<$Timeline> scheduler;

      public Resume(final Scheduler<$Timeline> scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler);
      }
    }

    final class Abort<$Timeline> implements TaskRequest<$Timeline> {}
  }

  /*sealed*/ interface TaskResponse<$Timeline> {
    final class Success<$Timeline> implements TaskResponse<$Timeline> {
      public final TaskStatus<$Timeline> status;

      public Success(final TaskStatus<$Timeline> status) {
        this.status = Objects.requireNonNull(status);
      }
    }

    final class Failure<$Timeline> implements TaskResponse<$Timeline> {
      public final Throwable failure;

      public Failure(final Throwable failure) {
        this.failure = Objects.requireNonNull(failure);
      }
    }
  }

  public static final class TaskFailureException extends RuntimeException {
    public final Throwable cause;

    private TaskFailureException(final Throwable cause) {
      super(cause);
      this.cause = cause;
    }
  }

  private static final TaskAbort TaskAbort = new TaskAbort();
  /**
   * A control-flow exception for quickly aborting a task which will never proceed any further.
   *
   * This exception extends Error instead of RuntimeException to reduce the likelihood that
   * it gets spuriously caught by an over-broad catch clause.
   */
  private static final class TaskAbort extends Error {
    public TaskAbort() {
      super(null, null, /* capture suppressed exceptions? */ true, /* capture stack trace? */ false);
    }
  }
}
