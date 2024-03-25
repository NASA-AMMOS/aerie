package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.function.Supplier;

public /*non-final*/ class ModelActions {
  protected ModelActions() {}

  /* package-local */
  static final Scoped<Context> context = Scoped.create();


  public static <T> TaskFactory<T> threaded(final Supplier<T> task) {
    return executor -> new ThreadedTask<>(executor, ModelActions.context, task);
  }

  public static TaskFactory<Unit> threaded(final Runnable task) {
    return threaded(() -> {
      task.run();
      return Unit.UNIT;
    });
  }

  public static <T> TaskFactory<T> replaying(final Supplier<T> task) {
    return executor -> new ReplayingTask<>(ModelActions.context, task);
  }

  public static TaskFactory<Unit> replaying(final Runnable task) {
    return replaying(() -> {
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

  public static void spawn(final Runnable task) {
    spawn(() -> {
      task.run();
      return Unit.UNIT;
    });
  }

  public static <T> void spawn(final TaskFactory<T> task) {
    context.get().spawn(task);
  }

  public static void call(final Runnable task) {
    call(threaded(task));
  }

  public static <T> void call(final Supplier<T> task) {
    call(threaded(task));
  }

  public static <T> void call(final TaskFactory<T> task) {
    context.get().call(task);
  }

  public static void tailCall(final Runnable task) {
    tailCall(threaded(task));
  }

  public static <T> void tailCall(final Supplier<T> task) {
    tailCall(threaded(task));
  }

  public static <T> void tailCall(final TaskFactory<T> task) {
    context.get().tailCall(task);
  }

  public static void defer(final Duration duration, final Runnable task) {
    spawn(replaying(() -> { delay(duration); spawn(task); }));
  }

  public static void defer(final Duration duration, final TaskFactory<?> task) {
    spawn(replaying(() -> { delay(duration); spawn(task); }));
  }

  public static void defer(final long quantity, final Duration unit, final Runnable task) {
    spawn(replaying(() -> { delay(quantity, unit); spawn(task); }));
  }

  public static void defer(final long quantity, final Duration unit, final TaskFactory<?> task) {
    spawn(replaying(() -> { delay(quantity, unit); spawn(task); }));
  }


  public static void delay(final Duration duration) {
    context.get().delay(duration);
  }

  public static void delay(final long quantity, final Duration unit) {
    delay(unit.times(quantity));
  }

  public static void waitUntil(final Condition condition) {
    context.get().waitUntil(condition);
  }
}
