package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class ThreadedTask<Input, Output> implements Task<Input, Output> {
  private final Executor executor;
  private final Scoped<Context> rootContext;
  private final Function<Input, Output> task;

  public ThreadedTask(final Executor executor, final Scoped<Context> rootContext, final Function<Input, Output> task) {
    this.executor = Objects.requireNonNull(executor);
    this.rootContext = Objects.requireNonNull(rootContext);
    this.task = Objects.requireNonNull(task);
  }

  @Override
  public TaskStatus<Output> step(final Scheduler scheduler, final Input input) {
    final var responseQueue = new ArrayBlockingQueue<Response<Output>>(1);
    final var context = new TaskContext<>(responseQueue, scheduler);

    // Use locals to avoid holding a reference on `this` from the thread's lambda.
    final var task = this.task;
    final var rootContext = this.rootContext;
    this.executor.execute(() -> {
      try (final var restore = rootContext.set(context)) {
        final var output = task.apply(input);
        responseQueue.add(new Response.Success<>(TaskStatus.completed(output)));
      } catch (final TaskAbort ex) {
        // Do nothing!
      } catch (final Throwable ex) {
        responseQueue.add(new Response.Failure<>(ex));
      }
    });

    return ThreadedTask.await(responseQueue);
  }

  public static <Output> TaskStatus<Output> await(final ArrayBlockingQueue<Response<Output>> responseQueue) {
    final Response<Output> response;
    try {
      response = responseQueue.take();
    } catch (final InterruptedException ex) {
      throw new Error("Merlin host unexpectedly interrupted", ex);
    }

    if (response instanceof Response.Success<Output> r) {
      return r.status();
    } else if (response instanceof Response.Failure<Output> r) {
      // We re-throw the received exception to avoid interfering with `catch` blocks
      //   that might be looking for this specific exception, but we add a new exception
      //   to its suppression list to provide a stack trace in this thread, too.
      final var ex = r.failure();
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
          Response.class.getCanonicalName(),
          response.getClass().getCanonicalName()));
    }
  }

  private static final class YieldedTask<Input, Output> implements Task<Input, Output> {
    private final ArrayBlockingQueue<Request<Input>> requestQueue;
    private final ArrayBlockingQueue<Response<Output>> responseQueue;

    public YieldedTask(
        final ArrayBlockingQueue<Request<Input>> requestQueue,
        final ArrayBlockingQueue<Response<Output>> responseQueue
    ) {
      this.requestQueue = Objects.requireNonNull(requestQueue);
      this.responseQueue = Objects.requireNonNull(responseQueue);
    }

    @Override
    public TaskStatus<Output> step(final Scheduler scheduler, final Input input) {
      try {
        this.requestQueue.put(new Request.Resume<>(scheduler, input));
      } catch (final InterruptedException ex) {
        throw new Error("Merlin host unexpectedly interrupted", ex);
      }

      return ThreadedTask.await(this.responseQueue);
    }

    @Override
    public void release() {
      try {
        this.requestQueue.put(new Request.Abort<>());
      } catch (final InterruptedException ex) {
        throw new Error("Merlin host unexpectedly interrupted", ex);
      }
    }
  }

  public sealed interface Request<Input> {
    record Resume<Input>(Scheduler scheduler, Input input) implements Request<Input> {}

    record Abort<Input>() implements Request<Input> {}
  }

  public sealed interface Response<Output> {
    record Success<Output>(TaskStatus<Output> status) implements Response<Output> {}

    record Failure<Output>(Throwable failure) implements Response<Output> {}
  }

  public static final class TaskFailureException extends RuntimeException {
    public TaskFailureException() {
      super("Observed task thread failure from driver thread");
    }
  }

  /**
   * A control-flow exception for quickly aborting a task which will never proceed any further.
   *
   * This exception extends Error instead of RuntimeException to reduce the likelihood that
   * it gets spuriously caught by an over-broad catch clause.
   */
  private static final class TaskAbort extends Error {
    public static final TaskAbort INSTANCE = new TaskAbort();

    public TaskAbort() {
      super(null, null, /* capture suppressed exceptions? */ true, /* capture stack trace? */ false);
    }
  }

  private static final class TaskContext<Output> implements Context {
    private final ArrayBlockingQueue<Response<Output>> responseQueue;

    private Scheduler scheduler;
    private boolean isAborting = false;

    public TaskContext(final ArrayBlockingQueue<Response<Output>> responseQueue, final Scheduler scheduler) {
      this.responseQueue = Objects.requireNonNull(responseQueue);
      this.scheduler = Objects.requireNonNull(scheduler);
    }

    @Override
    public ContextType getContextType() {
      return ContextType.Reacting;
    }

    @Override
    public <State> State ask(final CellId<State> cellId) {
      // If we're in the middle of aborting, just keep trying to bail out.
      if (this.isAborting) throw TaskAbort.INSTANCE;

      return this.scheduler.get(cellId);
    }

    @Override
    public <Event, Effect, State> CellId<State> allocate(
        final State initialState,
        final CellType<Effect, State> cellType,
        final Function<Event, Effect> interpretation,
        final Topic<Event> topic
    ) {
      throw new IllegalStateException("Cannot allocate during simulation");
    }

    @Override
    public <Event> void emit(final Event event, final Topic<Event> topic) {
      // If we're in the middle of aborting, just keep trying to bail out.
      if (this.isAborting) throw TaskAbort.INSTANCE;

      this.scheduler.emit(event, topic);
    }

    @Override
    public void spawn(final TaskFactory<Unit, ?> task) {
      // If we're in the middle of aborting, just keep trying to bail out.
      if (this.isAborting) throw TaskAbort.INSTANCE;

      this.scheduler.spawn(task);
    }

    @Override
    public <Midput> void call(final TaskFactory<Unit, Midput> child) {
      // If we're in the middle of aborting, just keep trying to bail out.
      if (this.isAborting) throw TaskAbort.INSTANCE;

      this.scheduler = null;
      final var request = this.<Midput>yield($ -> TaskStatus.calling(child, $));
      this.scheduler = request.scheduler();
    }

    @Override
    public void delay(final Duration duration) {
      // If we're in the middle of aborting, just keep trying to bail out.
      if (this.isAborting) throw TaskAbort.INSTANCE;

      this.scheduler = null;
      final var request = this.<Unit>yield($ -> TaskStatus.delayed(duration, $));
      this.scheduler = request.scheduler();
    }

    @Override
    public void waitUntil(final Condition condition) {
      // If we're in the middle of aborting, just keep trying to bail out.
      if (this.isAborting) throw TaskAbort.INSTANCE;

      this.scheduler = null;
      final var request = this.<Unit>yield($ -> TaskStatus.awaiting(condition, $));
      this.scheduler = request.scheduler();
    }

    private <Midput> Request.Resume<Midput> yield(final Function<Task<Midput, Output>, TaskStatus<Output>> status) {
      // Get the next request from the driver.
      final Request<Midput> request;
      try {
        final var requestQueue = new ArrayBlockingQueue<Request<Midput>>(1);
        final var response = status.apply(new YieldedTask<>(requestQueue, this.responseQueue));
        this.responseQueue.put(new Response.Success<>(response));
        request = requestQueue.take();
      } catch (final InterruptedException ex) {
        throw new Error("Merlin task unexpectedly interrupted", ex);
      }

      if (request instanceof Request.Resume<Midput> r) {
        // We've been told to continue executing.
        return r;
      } else if (request instanceof Request.Abort) {
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
        throw TaskAbort.INSTANCE;
      } else {
        throw new Error(String.format(
            "Unexpected variant of %s: %s",
            Request.class.getCanonicalName(),
            request.getClass().getCanonicalName()));
      }
    }
  }
}
