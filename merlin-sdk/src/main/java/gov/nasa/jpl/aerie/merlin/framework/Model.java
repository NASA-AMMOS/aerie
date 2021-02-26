package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class Model {
  private final Supplier<? extends Context<?>> context;

  protected Model(final Supplier<? extends Context<?>> context) {
    this.context = Objects.requireNonNull(context);
  }

  protected Model(final Context<?> context) {
    this(() -> context);
  }

  protected Model(final Registrar registrar) {
    this(registrar.getRootContext());
  }

  protected final String spawn(final Runnable task) {
    return this.context.get().spawn(task);
  }

  protected final String spawn(final String type, final Map<String, SerializedValue> arguments) {
    return this.context.get().spawn(type, arguments);
  }

  protected final void call(final Runnable task) {
    this.waitFor(this.spawn(task));
  }

  protected final void call(final String type, final Map<String, SerializedValue> arguments) {
    this.waitFor(this.spawn(type, arguments));
  }

  protected final String defer(final Duration duration, final Runnable task) {
    return this.context.get().defer(duration, task);
  }

  protected final String defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
    return this.context.get().defer(duration, type, arguments);
  }

  protected final String defer(final long quantity, final Duration unit, final Runnable task) {
    return this.defer(unit.times(quantity), task);
  }

  protected final String defer(final long quantity, final Duration unit, final String type, final Map<String, SerializedValue> arguments) {
    return this.defer(unit.times(quantity), type, arguments);
  }


  protected final void delay(final Duration duration) {
    this.context.get().delay(duration);
  }

  protected final void delay(final long quantity, final Duration unit) {
    this.delay(unit.times(quantity));
  }

  protected final void waitFor(final String id) {
    this.context.get().waitFor(id);
  }

  protected final void waitUntil(final Condition condition) {
    this.context.get().waitUntil(condition);
  }
}
