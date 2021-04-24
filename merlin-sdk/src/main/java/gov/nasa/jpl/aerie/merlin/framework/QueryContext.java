package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.Checkpoint;
import gov.nasa.jpl.aerie.merlin.protocol.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;

public final class QueryContext<$Schema> implements Context {
  private final Checkpoint<? extends $Schema> history;

  public QueryContext(final Checkpoint<? extends $Schema> history) {
    this.history = history;
  }

  @Override
  public <CellType> CellType ask(final Query<?, ?, CellType> query) {
    // SAFETY: All objects accessible within a single adaptation instance have the same brand.
    @SuppressWarnings("unchecked")
    final var brandedQuery = (Query<? super $Schema, ?, CellType>) query;

    return this.history.ask(brandedQuery);
  }

  @Override
  public <Event, Effect, CellType> Query<?, Event, CellType> allocate(
      final Projection<Event, Effect> projection,
      final Applicator<Effect, CellType> applicator)
  {
    throw new IllegalStateException("Cannot allocate in a query-only context");
  }

  @Override
  public <Event> void emit(final Event event, final Query<?, Event, ?> query) {
    throw new IllegalStateException("Cannot update simulation state in a query-only context");
  }

  @Override
  public String spawn(final TaskFactory task) {
    throw new IllegalStateException("Cannot schedule tasks in a query-only context");
  }

  @Override
  public String spawn(final String type, final Map<String, SerializedValue> arguments) {
    throw new IllegalStateException("Cannot schedule activities in a query-only context");
  }

  @Override
  public String defer(final Duration duration, final TaskFactory task) {
    throw new IllegalStateException("Cannot schedule tasks in a query-only context");
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
  public void waitUntil(final Condition condition) {
    throw new IllegalStateException("Cannot yield in a query-only context");
  }
}
