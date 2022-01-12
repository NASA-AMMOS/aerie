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


  public static void spawn(final Runnable task) {
    spawn(threaded(task));
  }

  public static void spawn(final Context.TaskFactory task) {
    context.get().spawn(task);
  }

  public static void spawn(final String type, final Map<String, SerializedValue> arguments) {
    context.get().spawn(type, arguments);
  }

  public static void call(final Runnable task) {
    call(threaded(task));
  }

  public static void call(final Context.TaskFactory task) {
    context.get().call(task);
  }

  public static void call(final String type, final Map<String, SerializedValue> arguments) {
    context.get().call(type, arguments);
  }

  public static void defer(final Duration duration, final Runnable task) {
    defer(duration, threaded(task));
  }

  public static void defer(final Duration duration, final Context.TaskFactory task) {
    context.get().defer(duration, task);
  }

  public static void defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
    context.get().defer(duration, type, arguments);
  }

  public static void defer(final long quantity, final Duration unit, final Runnable task) {
    defer(unit.times(quantity), threaded(task));
  }

  public static void defer(final long quantity, final Duration unit, final String type, final Map<String, SerializedValue> arguments) {
    defer(unit.times(quantity), type, arguments);
  }

  public static void defer(final long quantity, final Duration unit, final Context.TaskFactory task) {
    context.get().defer(unit.times(quantity), task);
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
