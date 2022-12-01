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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ReplayingTask<Input, Output> implements Task<Input, Output> {
  private final Scoped<Context> rootContext;
  private final Function<Input, Output> task;

  public ReplayingTask(final Scoped<Context> rootContext, final Function<Input, Output> task) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.task = Objects.requireNonNull(task);
  }

  @Override
  public TaskStatus<Output> step(final Scheduler scheduler, final Input input) {
    // Use locals to avoid holding a reference on `this` from the thread's lambda.
    final var rootContext = this.rootContext;
    final var task = this.task;

    return TaskContext.replay(new History(), scheduler, (context) -> {
      try (final var restore = rootContext.set(context)) {
        return task.apply(input);
      }
    });
  }

  private static final class YieldedTask<Input, Output> implements Task<Input, Output> {
    private final History history;
    private final Function<Context, Output> task;
    private final KeepDiscard keepDiscard;

    public YieldedTask(final History history, final Function<Context, Output> task, final KeepDiscard keepDiscard) {
      this.history = Objects.requireNonNull(history);
      this.task = Objects.requireNonNull(task);
      this.keepDiscard = Objects.requireNonNull(keepDiscard);
    }

    @Override
    public TaskStatus<Output> step(final Scheduler scheduler, final Input input) {
      switch (this.keepDiscard) {
        case Keep -> this.history.remember(() -> input);
        case Discard -> this.history.forget(() -> {});
      }

      return TaskContext.replay(this.history, scheduler, this.task);
    }
  }

  private enum KeepDiscard { Keep, Discard }

  private static final class History {
    private final List<Object> memory = new ArrayList<>();
    private int ignoredInputs = 0;

    private int memoryIndex = 0;
    private int ignoredIndex = 0;

    public void rewind() {
      this.memoryIndex = 0;
      this.ignoredIndex = 0;
    }

    public <T> T remember(final Supplier<T> action) {
      T value;
      if (this.memoryIndex < this.memory.size()) {
        // SAFETY: Tasks are deterministic, and last time we did this action, we cached a value of type `T`.
        @SuppressWarnings("unchecked")
        final var tmp = (T) this.memory.get(this.memoryIndex);
        value = tmp;
      } else {
        value = action.get();
        this.memory.add(value);
      }

      this.memoryIndex += 1;
      return value;
    }

    public void forget(final Runnable action) {
      if (this.ignoredIndex < this.ignoredInputs) {
        // Do nothing
      } else {
        action.run();
        this.ignoredInputs += 1;
      }

      this.ignoredIndex += 1;
    }
  }

  /**
   * A control-flow exception for quickly escaping from a task which cannot yet proceed any further.
   *
   * <p> This exception extends Error instead of RuntimeException to reduce the likelihood that
   * it gets spuriously caught by an over-broad catch clause. </p>
   */
  private static final class Yield extends Error {
    public static final Yield INSTANCE = new Yield();

    public Yield() {
      super(null, null, /* capture suppressed exceptions? */ false, /* capture stack trace? */ false);
    }
  }

  private static final class TaskContext<Output> implements Context {
    private final Function<Context, Output> task;
    private final History history;

    private final Scheduler scheduler;
    private TaskStatus<Output> status = null;
    private boolean isYielding = false;

    public TaskContext(final Function<Context, Output> task, final Scheduler scheduler, final History history) {
      this.task = Objects.requireNonNull(task);
      this.scheduler = Objects.requireNonNull(scheduler);
      this.history = Objects.requireNonNull(history);
    }

    public TaskContext(final Function<Context, Output> task, final Scheduler scheduler) {
      this(task, scheduler, new History());
    }

    public static <Output>
    TaskStatus<Output> replay(
        final History history,
        final Scheduler scheduler,
        final Function<Context, Output> task
    ) {
      history.rewind();
      final var context = new TaskContext<>(task, scheduler, history);
      try {
        return TaskStatus.completed(task.apply(context));
      } catch (final Yield ignored) {
        // TODO: Extract the new TaskStatus from `context`.
        return context.status;
      }
    }

    @Override
    public ContextType getContextType() {
      if (this.isYielding) throw Yield.INSTANCE;

      return ContextType.Reacting;
    }

    @Override
    public <State> State ask(final CellId<State> cellId) {
      if (this.isYielding) throw Yield.INSTANCE;

      return this.history.remember(() -> this.scheduler.get(cellId));
    }

    @Override
    public <Event, Effect, State>
    CellId<State> allocate(
        final State initialState,
        final CellType<Effect, State> cellType,
        final Function<Event, Effect> interpretation,
        final Topic<Event> topic
    ) {
      if (this.isYielding) throw Yield.INSTANCE;

      throw new IllegalStateException("Cannot allocate during simulation");
    }

    @Override
    public <Event> void emit(final Event event, final Topic<Event> topic) {
      if (this.isYielding) throw Yield.INSTANCE;

      this.history.forget(() -> this.scheduler.emit(event, topic));
    }

    @Override
    public <Input> void spawn(final TaskFactory<Input, ?> child, final Input input) {
      if (this.isYielding) throw Yield.INSTANCE;

      this.history.forget(() -> this.scheduler.spawn(child, input));
    }

    @Override
    public <Input, Midput> Midput call(final TaskFactory<Input, Midput> child, final Input input) {
      if (this.isYielding) throw Yield.INSTANCE;

      return this.history.remember(() -> {
        this.status = TaskStatus.calling(input, child, new YieldedTask<>(this.history, this.task, KeepDiscard.Keep));
        this.isYielding = true;
        throw Yield.INSTANCE;
      });
    }

    @Override
    public void delay(final Duration duration) {
      if (this.isYielding) throw Yield.INSTANCE;

      this.history.forget(() -> {
        this.status = TaskStatus.delayed(duration, new YieldedTask<>(this.history, this.task, KeepDiscard.Discard));
        this.isYielding = true;
        throw Yield.INSTANCE;
      });
    }

    @Override
    public void waitUntil(final Condition condition) {
      if (this.isYielding) throw Yield.INSTANCE;

      this.history.forget(() -> {
        this.status = TaskStatus.awaiting(condition, new YieldedTask<>(this.history, this.task, KeepDiscard.Discard));
        this.isYielding = true;
        throw Yield.INSTANCE;
      });
    }
  }
}
