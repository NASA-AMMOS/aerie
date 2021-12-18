package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.function.Supplier;

public /*non-final*/ class ModelActions {
  protected ModelActions() {}

  /* package-local */
  static final Scoped<Context> context = Scoped.create();


  public static <T> Context.TaskFactory threaded(final Supplier<T> task) {
    return executor -> new ThreadedTask<>(executor, ModelActions.context, task);
  }

  public static Context.TaskFactory threaded(final Runnable task) {
    return threaded(supplierOfRunnable(task));
  }

  public static <T> Context.TaskFactory replaying(final Supplier<T> task) {
    return executor -> new ReplayingTask<>(executor, ModelActions.context, task);
  }

  public static Context.TaskFactory replaying(final Runnable task) {
    return replaying(supplierOfRunnable(task));
  }


  public static <T> Scheduler.TaskIdentifier spawn(final Supplier<T> task) {
    return spawn(threaded(task));
  }

  public static Scheduler.TaskIdentifier spawn(final Runnable task) {
    return spawn(supplierOfRunnable(task));
  }

  private static Supplier<Void> supplierOfRunnable(final Runnable task) {
    return () -> {
      task.run();
      return null;
    };
  }

  public static Scheduler.TaskIdentifier spawn(final Context.TaskFactory task) {
    return context.get().spawn(task);
  }

  public static Scheduler.TaskIdentifier spawn(final String type, final Map<String, SerializedValue> arguments) {
    return context.get().spawn(type, arguments);
  }

  public static void call(final Runnable task) {
    call(threaded(task));
  }

  public static <T> void call(final Supplier<T> task) {
    call(threaded(task));
  }

  public static void call(final Context.TaskFactory task) {
    waitFor(spawn(task));
  }

  public static void call(final String type, final Map<String, SerializedValue> arguments) {
    waitFor(spawn(type, arguments));
  }

  public static Scheduler.TaskIdentifier defer(final Duration duration, final Runnable task) {
    return defer(duration, threaded(task));
  }

  public static Scheduler.TaskIdentifier defer(final Duration duration, final Context.TaskFactory task) {
    return context.get().defer(duration, task);
  }

  public static Scheduler.TaskIdentifier defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
    return context.get().defer(duration, type, arguments);
  }

  public static Scheduler.TaskIdentifier defer(final long quantity, final Duration unit, final Runnable task) {
    return defer(unit.times(quantity), threaded(task));
  }

  public static Scheduler.TaskIdentifier defer(final long quantity, final Duration unit, final String type, final Map<String, SerializedValue> arguments) {
    return defer(unit.times(quantity), type, arguments);
  }

  public static Scheduler.TaskIdentifier defer(final long quantity, final Duration unit, final Context.TaskFactory task) {
    return context.get().defer(unit.times(quantity), task);
  }


  public static void delay(final Duration duration) {
    context.get().delay(duration);
  }

  public static void delay(final long quantity, final Duration unit) {
    delay(unit.times(quantity));
  }

  public static void waitFor(final Scheduler.TaskIdentifier id) {
    context.get().waitFor(id);
  }

  public static void waitUntil(final Condition condition) {
    context.get().waitUntil(condition);
  }
}
