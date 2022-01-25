package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public /*non-final*/ class ModelActions {
  protected ModelActions() {}

  /* package-local */
  static final Scoped<Context> context = Scoped.create();


  public static Context.TaskFactory threaded(final Runnable task) {
    return executor -> new ThreadedTask(executor, ModelActions.context, task);
  }
  public static Context.TaskFactory replaying(final Runnable task) {
    return executor -> new ReplayingTask(executor, ModelActions.context, task);
  }


  public static String spawn(final Runnable task) {
    return spawn(threaded(task));
  }

  public static String spawn(final Context.TaskFactory task) {
    return context.get().spawn(task);
  }

  public static String spawn(final String type, final Map<String, SerializedValue> arguments) {
    return context.get().spawn(type, arguments);
  }

  public static void call(final Runnable task) {
    call(threaded(task));
  }

  public static void call(final Context.TaskFactory task) {
    waitFor(spawn(task));
  }

  public static void call(final String type, final Map<String, SerializedValue> arguments) {
    waitFor(spawn(type, arguments));
  }

  public static String defer(final Duration duration, final Runnable task) {
    return spawn(replaying(() -> { delay(duration); spawn(task); }));
  }

  public static String defer(final Duration duration, final Context.TaskFactory task) {
    return spawn(replaying(() -> { delay(duration); spawn(task); }));
  }

  public static String defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
    return spawn(replaying(() -> { delay(duration); spawn(type, arguments); }));
  }

  public static String defer(final long quantity, final Duration unit, final Runnable task) {
    return spawn(replaying(() -> { delay(quantity, unit); spawn(task); }));
  }

  public static String defer(final long quantity, final Duration unit, final Context.TaskFactory task) {
    return spawn(replaying(() -> { delay(quantity, unit); spawn(task); }));
  }

  public static String defer(final long quantity, final Duration unit, final String type, final Map<String, SerializedValue> arguments) {
    return spawn(replaying(() -> { delay(quantity, unit); spawn(type, arguments); }));
  }


  public static void delay(final Duration duration) {
    context.get().delay(duration);
  }

  public static void delay(final long quantity, final Duration unit) {
    delay(unit.times(quantity));
  }

  public static void waitFor(final String id) {
    context.get().waitFor(id);
  }

  public static void waitUntil(final Condition condition) {
    context.get().waitUntil(condition);
  }
}
