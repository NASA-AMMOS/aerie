package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;

public final class ModelActions {
  private ModelActions() {}

  /* package-local */
  static final Scoped<Context> context = Scoped.create();


  public static String spawn(final Runnable task) {
    return context.get().spawn(task);
  }

  public static String spawn(final String type, final Map<String, SerializedValue> arguments) {
    return context.get().spawn(type, arguments);
  }

  public static void call(final Runnable task) {
    waitFor(spawn(task));
  }

  public static void call(final String type, final Map<String, SerializedValue> arguments) {
    waitFor(spawn(type, arguments));
  }

  public static String defer(final Duration duration, final Runnable task) {
    return context.get().defer(duration, task);
  }

  public static String defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
    return context.get().defer(duration, type, arguments);
  }

  public static String defer(final long quantity, final Duration unit, final Runnable task) {
    return defer(unit.times(quantity), task);
  }

  public static String defer(final long quantity, final Duration unit, final String type, final Map<String, SerializedValue> arguments) {
    return defer(unit.times(quantity), type, arguments);
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
