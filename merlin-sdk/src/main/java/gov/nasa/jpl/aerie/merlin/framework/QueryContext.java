package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.CompoundCondition;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;

public final class QueryContext<$Schema> implements Context<$Schema> {
  private final History<? extends $Schema> history;

  public QueryContext(final History<? extends $Schema> history) {
    this.history = history;
  }

  @Override
  public History<? extends $Schema> now() {
    return this.history;
  }

  @Override
  public <Event> void emit(final Event event, final Query<? super $Schema, Event, ?> query) {
    throw new IllegalStateException("Cannot update simulation state in a query-only context");
  }

  @Override
  public String spawn(final String type, final Map<String, SerializedValue> arguments) {
    throw new IllegalStateException("Cannot schedule activities in a query-only context");
  }

  @Override
  public String defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
    throw new IllegalStateException("Cannot schedule activities in a query-only context");
  }

  @Override
  public void delay(final Duration duration) {
    throw new IllegalStateException("Cannot yield in a query-only context");
  }

  @Override
  public void waitFor(final String id) {
    throw new IllegalStateException("Cannot yield in a query-only context");
  }

  @Override
  public void waitUntil(final CompoundCondition<?> condition) {
    throw new IllegalStateException("Cannot yield in a query-only context");
  }
}
