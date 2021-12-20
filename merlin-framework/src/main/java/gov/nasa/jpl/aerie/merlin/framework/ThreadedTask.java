package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;
import java.util.concurrent.ExecutorService;

public final class ThreadedTask<ReturnType> implements Task {
  private final Scoped<Context> rootContext;
  private final Supplier<ReturnType> task;
  private final ExecutorService executor;

  private final ArrayBlockingQueue<TaskRequest> hostToTask = new ArrayBlockingQueue<>(1);
  private final ArrayBlockingQueue<TaskResponse> taskToHost = new ArrayBlockingQueue<>(1);

  private Lifecycle lifecycle = Lifecycle.Inactive;
  private ReturnType returnValue;

  public ThreadedTask(final ExecutorService executor, final Scoped<Context> rootContext, final Supplier<ReturnType> task) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.task = Objects.requireNonNull(task);
    this.executor = Objects.requireNonNull(executor);
  }

  @SuppressWarnings("unchecked")
  @Override
  public TaskStatus step(final Scheduler scheduler) {
    try {
      if (this.lifecycle == Lifecycle.Terminated) {
        return TaskStatus.completed(this.returnValue);
      } else if (this.lifecycle == Lifecycle.Inactive) {
        this.lifecycle = Lifecycle.Running;
        beginAsync();
      }

      // TODO: Add a (configurable) timeout to the `take()` call.
      //   It should be sufficiently long as to allow the user-defined task to do its job.
      //   The `put()` call is fine -- we know the thread will immediately wait
      //   for a new request as soon as it puts a response to the last request.
      // TODO: Track metrics for how long a task runs before responding.
      //   This will help to tune the timeout.
      this.hostToTask.put(new TaskRequest.Resume(scheduler));
      final var response = this.taskToHost.take();

      if (response instanceof TaskResponse.Success successResponse) {
        final var status = successResponse.status;

        if (status instanceof TaskStatus.Completed completed) {
          this.lifecycle = Lifecycle.Terminated;
          // SAFETY: This TaskStatus is constructed by this class, using the same ReturnType.
          this.returnValue = (ReturnType) completed.returnValue();
        }

        return status;
      } else if (response instanceof TaskResponse.Failure failureResponse) {
        this.lifecycle = Lifecycle.Terminated;

        // We re-throw the received exception to avoid interfering with `catch` blocks
        //   that might be looking for this specific exception, but we add a new exception
        //   to its suppression list to provide a stack trace in this thread, too.
        final var ex = failureResponse.failure;
        ex.addSuppressed(new TaskFailureException());

        // This exception shouldn't be a checked exception, but we have to prove it to Java.
        if (ex instanceof RuntimeException runtimeException) {
          throw runtimeException;
        } else if (ex instanceof Error error) {
          throw error;
        } else {
          throw new RuntimeException("Unexpected checked exception escaped from task thread", ex);
        }
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

  private void beginAsync() {
    final var handle = new ThreadedTaskHandle();

    this.executor.execute(() -> {
      final TaskRequest request;
      try {
        request = ThreadedTask.this.hostToTask.take();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }

      TaskResponse response;
      try {
        response = handle.run(request);
      } catch (final Throwable ex) {
        response = new TaskResponse.Failure(ex);
      }

      try {
        ThreadedTask.this.taskToHost.put(response);
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }
    });
  }

  @Override
  public void reset() {
    if (this.lifecycle == Lifecycle.Running) {
      try {
        // TODO: Add a (configurable) timeout to the `take()` call.
        //   This timeout can be (much) shorter than the one in `ThreadedTask.step()`.
        //   The `put()` call is fine -- we know the thread will immediately wait
        //   for a new request as soon as it puts a response to the last request.
        this.hostToTask.put(new TaskRequest.Abort());
        final var ignored = this.taskToHost.take();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin host unexpectedly interrupted", ex);
      }
    }

    this.lifecycle = Lifecycle.Inactive;
  }

  private final class ThreadedTaskHandle implements TaskHandle {
    private boolean isAborting = false;

    public TaskResponse run(final TaskRequest request) {
      if (request instanceof TaskRequest.Resume resume) {
        final var scheduler = resume.scheduler;

        final var context = new ThreadedReactionContext(
            ThreadedTask.this.executor,
            ThreadedTask.this.rootContext,
            scheduler,
            this);

        try (final var restore = ThreadedTask.this.rootContext.set(context)) {
          ThreadedTask.this.returnValue = ThreadedTask.this.task.get();
          return new TaskResponse.Success(TaskStatus.completed(ThreadedTask.this.returnValue));
        } catch (final TaskAbort ex) {
          return new TaskResponse.Success(TaskStatus.completed());
        } catch (final Throwable ex) {
          return new TaskResponse.Failure(ex);
        }
      } else if (request instanceof TaskRequest.Abort) {
        return new TaskResponse.Success(TaskStatus.completed());
      } else {
        throw new Error(String.format(
            "Unexpected variant of %s: %s",
            TaskRequest.class.getCanonicalName(),
            request.getClass().getCanonicalName()));
      }
    }

    @Override
    public Scheduler yield(final TaskStatus status) {
      // If we're in the middle of aborting, just keep trying to bail out.
      if (this.isAborting) throw TaskAbort;

      // Get the next request from the driver.
      final TaskRequest request;
      try {
        ThreadedTask.this.taskToHost.put(new TaskResponse.Success(status));
        request = ThreadedTask.this.hostToTask.take();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }

      if (request instanceof TaskRequest.Resume resumeRequest) {
        // We've been told to continue executing.
        return resumeRequest.scheduler;
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

  private enum Lifecycle { Inactive, Running, Terminated }

  /*sealed*/ interface TaskRequest {
    final class Resume implements TaskRequest {
      public final Scheduler scheduler;

      public Resume(final Scheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler);
      }
    }

    final class Abort implements TaskRequest {}
  }

  /*sealed*/ interface TaskResponse {
    final class Success implements TaskResponse {
      public final TaskStatus status;

      public Success(final TaskStatus status) {
        this.status = Objects.requireNonNull(status);
      }
    }

    final class Failure implements TaskResponse {
      public final Throwable failure;

      public Failure(final Throwable failure) {
        this.failure = Objects.requireNonNull(failure);
      }
    }
  }

  public static final class TaskFailureException extends RuntimeException {
    public TaskFailureException() {
      super("Observed task thread failure from driver thread");
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
