package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.function.Function;
import java.util.function.Supplier;

public class TrampoliningTask<Return> implements Task<Return> {
  private final Scoped<Context> rootContext;
  private final Supplier<TrampoliningTaskStatus<Return>> task;

  public TrampoliningTask(final Scoped<Context> rootContext, Supplier<TrampoliningTaskStatus<Return>> task) {
    this.rootContext = rootContext;
    this.task = task;
  }

  public static <Return> Supplier<TrampoliningTaskStatus<Return>> repeating(Supplier<RepeatingTaskStatus<Return>> task) {
    return () -> task.get().match(
        completed -> new TrampoliningTaskStatus.Completed<>(completed.result),
        delayed -> new TrampoliningTaskStatus.Delayed<>(delayed.delay, repeating(task)),
        awaiting -> new TrampoliningTaskStatus.AwaitingCondition<>(awaiting.condition, repeating(task)));
  }

  @Override
  public TaskStatus<Return> step(final Scheduler scheduler) {
    final var context = new ThreadedReactionContext(rootContext, scheduler, new TrampoliningTaskHandle());

    try (final var restore = this.rootContext.set(context)) {
      final var status = this.task.get();

      return status.match(
          completed -> TaskStatus.completed(completed.result),
          delayed -> TaskStatus.delayed(delayed.delay, new TrampoliningTask<>(rootContext, delayed.continuation)),
          awaiting -> TaskStatus.awaiting(
              (now, atLatest) -> {
                try (final var restoreQuery = this.rootContext.set(new QueryContext(now))) {
                  return awaiting.condition.nextSatisfied(true, Duration.ZERO, atLatest);
                }
              },
              new TrampoliningTask<>(rootContext, awaiting.continuation)));
    }
  }

  public sealed interface TrampoliningTaskStatus<Return> {
    <T> T match(Function<Completed<Return>, T> f, Function<Delayed<Return>, T> g, Function<AwaitingCondition<Return>, T> h);

    record Completed<Return>(Return result) implements TrampoliningTaskStatus<Return> {
      @Override
      public <T> T match(
          final Function<Completed<Return>, T> f,
          final Function<Delayed<Return>, T> g,
          final Function<AwaitingCondition<Return>, T> h)
      {
        return f.apply(this);
      }
    }
    record Delayed<Return>(Duration delay, Supplier<TrampoliningTaskStatus<Return>> continuation) implements TrampoliningTaskStatus<Return> {
      @Override
      public <T> T match(
          final Function<Completed<Return>, T> f,
          final Function<Delayed<Return>, T> g,
          final Function<AwaitingCondition<Return>, T> h)
      {
        return g.apply(this);
      }
    }
    record AwaitingCondition<Return>(Condition condition, Supplier<TrampoliningTaskStatus<Return>> continuation) implements TrampoliningTaskStatus<Return> {
      @Override
      public <T> T match(
          final Function<Completed<Return>, T> f,
          final Function<Delayed<Return>, T> g,
          final Function<AwaitingCondition<Return>, T> h)
      {
        return h.apply(this);
      }
    }
  }

  public sealed interface RepeatingTaskStatus<Return> {
    <T> T match(Function<Completed<Return>, T> f, Function<Delayed<Return>, T> g, Function<AwaitingCondition<Return>, T> h);

    record Completed<Return>(Return result) implements RepeatingTaskStatus<Return> {
      @Override
      public <T> T match(
          final Function<Completed<Return>, T> f,
          final Function<Delayed<Return>, T> g,
          final Function<AwaitingCondition<Return>, T> h)
      {
        return f.apply(this);
      }
    }
    record Delayed<Return>(Duration delay) implements RepeatingTaskStatus<Return> {
      @Override
      public <T> T match(
          final Function<Completed<Return>, T> f,
          final Function<Delayed<Return>, T> g,
          final Function<AwaitingCondition<Return>, T> h)
      {
        return g.apply(this);
      }
    }
    record AwaitingCondition<Return>(Condition condition) implements RepeatingTaskStatus<Return> {
      @Override
      public <T> T match(
          final Function<Completed<Return>, T> f,
          final Function<Delayed<Return>, T> g,
          final Function<AwaitingCondition<Return>, T> h)
      {
        return h.apply(this);
      }
    }

    static <Return> RepeatingTaskStatus<Return> completed(Return value) {
      return new Completed<>(value);
    }
    static <Return> RepeatingTaskStatus<Return> delayed(Duration delay) {
      return new Delayed<>(delay);
    }
    static <Return> RepeatingTaskStatus<Return> awaiting(Condition condition) {
      return new AwaitingCondition<>(condition);
    }
  }

  // Trampolining tasks don't support interrupting the flow of control.
  private static final class TrampoliningTaskHandle implements TaskHandle {
    @Override
    public Scheduler delay(final Duration delay) {
      throw new RuntimeException("delay is not supported from within a trampolining task");
    }

    @Override
    public Scheduler call(final TaskFactory<?> child) {
      throw new RuntimeException("call is not supported from within a trampolining task");
    }

    @Override
    public Scheduler await(final gov.nasa.jpl.aerie.merlin.protocol.model.Condition condition) {
      throw new RuntimeException("await is not supported from within a trampolining task");
    }
  }
}
