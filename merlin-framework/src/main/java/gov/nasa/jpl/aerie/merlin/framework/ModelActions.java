package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.function.Function;
import java.util.function.Supplier;

public /*non-final*/ class ModelActions {
  protected ModelActions() {}

  /* package-local */
  static final Scoped<Context> context = Scoped.create();


  public static <Input, Output> TaskFactory<Input, Output> threaded(final Function<Input, Output> task) {
    return executor -> new ThreadedTask<>(executor, ModelActions.context, task);
  }

  public static <T> TaskFactory<Unit, T> threaded(final Supplier<T> task) {
    return threaded($ -> task.get());
  }

  public static TaskFactory<Unit, Unit> threaded(final Runnable task) {
    return threaded($ -> {
      task.run();
      return Unit.UNIT;
    });
  }

  public static <Input, Output> TaskFactory<Input, Output> replaying(final Function<Input, Output> task) {
    return executor -> new ReplayingTask<>(ModelActions.context, task);
  }

  public static <T> TaskFactory<Unit, T> replaying(final Supplier<T> task) {
    return replaying($ -> task.get());
  }

  public static TaskFactory<Unit, Unit> replaying(final Runnable task) {
    return replaying($ -> {
      task.run();
      return Unit.UNIT;
    });
  }


  public static <T> void emit(final T event, final Topic<T> topic) {
    context.get().emit(event, topic);
  }


  public static <T> void spawn(final Supplier<T> task) {
    spawn(threaded(task));
  }

  public static <I, T> void spawn(final TaskFactory<I, T> task, final I input) {
    context.get().spawn(task, input);
  }

  public static <T> void spawn(final TaskFactory<Unit, T> task) {
    spawn(task, Unit.UNIT);
  }

  public static void spawn(final Runnable task) {
    spawn(threaded(task));
  }

  public static <I, T> T call(final TaskFactory<I, T> task, final I input) {
    return context.get().call(task, input);
  }

  public static <T> T call(final TaskFactory<Unit, T> task) {
    return call(task, Unit.UNIT);
  }

  public static <T> T call(final Supplier<T> task) {
    return call(threaded(task));
  }

  public static void call(final Runnable task) {
    call(threaded(task));
  }

  public static <I> void defer(final Duration duration, final TaskFactory<I, ?> task, final I input) {
    spawn(task.butFirst(Task.delaying(duration)), input);
  }

  public static void defer(final Duration duration, final TaskFactory<Unit, ?> task) {
    defer(duration, task, Unit.UNIT);
  }

  public static void defer(final Duration duration, final Runnable task) {
    defer(duration, threaded(task));
  }

  public static void defer(final long quantity, final Duration unit, final Runnable task) {
    defer(Duration.of(quantity, unit), task);
  }

  public static void defer(final long quantity, final Duration unit, final TaskFactory<Unit, ?> task) {
    defer(Duration.of(quantity, unit), task);
  }

  public static <I> void defer(final long quantity, final Duration unit, final TaskFactory<I, ?> task, final I input) {
    defer(Duration.of(quantity, unit), task, input);
  }


  public static void delay(final Duration duration) {
    context.get().delay(duration);
  }

  public static void delay(final long quantity, final Duration unit) {
    delay(unit.times(quantity));
  }

  public static void waitUntil(final Condition condition) {
    context.get().waitUntil((now, atLatest) -> {
      try (final var restore = ModelActions.context.set(new QueryContext(now))) {
        return condition.nextSatisfied(true, Duration.ZERO, atLatest);
      }
    });
  }
}
