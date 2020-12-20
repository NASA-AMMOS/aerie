package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Map;
import java.util.Objects;

public abstract class Module<$Schema> {
  private final Context<$Schema> context;

  protected Module(final Context<$Schema> context) {
    this.context = Objects.requireNonNull(context);
  }

  protected Module(final ResourcesBuilder.Cursor<$Schema> builder) {
    this(builder.getRootContext());
  }


  protected final History<? extends $Schema> now() {
    return this.context.now();
  }


  protected final <Event> void emit(final Event event, final Query<? super $Schema, Event, ?> query) {
    this.context.emit(event, query);
  }

  protected final String spawn(final String type, final Map<String, SerializedValue> arguments) {
    return this.context.spawn(type, arguments);
  }

  protected final void call(final String type, final Map<String, SerializedValue> arguments) {
    this.waitFor(this.spawn(type, arguments));
  }

  protected final String defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
    return this.context.defer(duration, type, arguments);
  }

  protected final String defer(final long quantity, final Duration unit, final String type, final Map<String, SerializedValue> arguments) {
    return this.defer(unit.times(quantity), type, arguments);
  }


  protected final void delay(final Duration duration) {
    this.context.delay(duration);
  }

  protected final void delay(final long quantity, final Duration unit) {
    this.delay(unit.times(quantity));
  }

  protected final void waitFor(final String id) {
    this.context.waitFor(id);
  }

  protected final void waitUntil(final Condition<$Schema> condition) {
    this.context.waitUntil(condition);
  }
}
