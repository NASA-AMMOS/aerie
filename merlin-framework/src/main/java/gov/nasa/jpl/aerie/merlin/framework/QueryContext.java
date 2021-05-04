package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.Query;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;

import java.util.Map;

public final class QueryContext<$Schema> implements Context {
  private final Querier<? extends $Schema> querier;

  public QueryContext(final Querier<? extends $Schema> querier) {
    this.querier = querier;
  }

  @Override
  public <CellType> CellType ask(final Query<?, ?, CellType> query) {
    // SAFETY: All objects accessible within a single adaptation instance have the same brand.
    @SuppressWarnings("unchecked")
    final var brandedQuery = (Query<$Schema, ?, CellType>) query;

    return this.querier.getState(brandedQuery);
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
