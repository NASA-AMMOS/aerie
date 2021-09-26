package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

public final class ThreadedTask<$Timeline> implements Task<$Timeline> {
  private final Scoped<Context> rootContext;
  private final Runnable task;

  private final ArrayBlockingQueue<TaskRequest<$Timeline>> hostToTask = new ArrayBlockingQueue<>(1);
  private final ArrayBlockingQueue<TaskResponse<$Timeline>> taskToHost = new ArrayBlockingQueue<>(1);

  private Thread thread = null;
  private boolean isTerminated = false;
  private final ReactionContext.Memory memory = new ReactionContext.Memory(new ArrayList<>(), new MutableInt(0));

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

      // TODO: Add a (configurable) timeout to the `take()` call.
      //   It should be sufficiently long as to allow the user-defined task to do its job.
      //   The `put()` call is fine -- we know the thread will immediately wait
      //   for a new request as soon as it puts a response to the last request.
      // TODO: Track metrics for how long a task runs before responding.
      //   This will help to tune the timeout.
      this.hostToTask.put(new TaskRequest.Resume<>(scheduler));
      final var response = this.taskToHost.take();

      if (response instanceof TaskResponse.Success) {
        final var status = ((TaskResponse.Success<$Timeline>) response).status;

        this.isTerminated = (status instanceof TaskStatus.Completed);

        return status;
      } else if (response instanceof TaskResponse.Failure) {
        this.thread = null;
        this.isTerminated = true;

        // We re-throw the received exception to avoid interfering with `catch` blocks
        //   that might be looking for this specific exception, but we add a new exception
        //   to its suppression list to provide a stack trace in this thread, too.
        final var ex = ((TaskResponse.Failure<$Timeline>) response).failure;
        ex.addSuppressed(new TaskFailureException());

        // This exception shouldn't be a checked exception, but we have to prove it to Java.
        if (ex instanceof RuntimeException) {
          throw (RuntimeException) ex;
        } else if (ex instanceof Error) {
          throw (Error) ex;
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

  @Override
  public void reset() {
    if (this.thread != null) {
      try {
        // TODO: Add a (configurable) timeout to the `take()` call.
        //   This timeout can be (much) shorter than the one in `ThreadedTask.step()`.
        //   The `put()` call is fine -- we know the thread will immediately wait
        //   for a new request as soon as it puts a response to the last request.
        this.hostToTask.put(new TaskRequest.Abort<>());
        final var ignored = this.taskToHost.take();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin host unexpectedly interrupted", ex);
      }
    }

    this.thread = null;
    this.isTerminated = false;
    this.memory.clear();
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

    // TODO: Set a thread name so that activity threads can easily be distinguished
    //   from within tools like Java Flight Recorder.

    return thread;
  }

  private final class ThreadedTaskHandle implements TaskHandle<$Timeline> {
    private boolean isAborting = false;

    public TaskResponse<$Timeline> run(final TaskRequest<$Timeline> request) {
      if (request instanceof TaskRequest.Resume) {
        final var scheduler = ((TaskRequest.Resume<$Timeline>) request).scheduler;

        final var context = new ReactionContext<>(
            ThreadedTask.this.rootContext,
            ThreadedTask.this.memory,
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
