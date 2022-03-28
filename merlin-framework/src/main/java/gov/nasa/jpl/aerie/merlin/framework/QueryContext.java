package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.function.Function;

public final class QueryContext implements Context {
  private final Querier querier;

  public QueryContext(final Querier querier) {
    this.querier = querier;
  }

  @Override
  public ContextType getContextType() {
    return ContextType.Querying;
  }

  @Override
  public <CellType> CellType ask(final Query<?, CellType> query) {
    return this.querier.getState(query);
  }

  @Override
  public <Event, Effect, CellType> Query<Event, CellType> allocate(
      final CellType initialState,
      final Applicator<Effect, CellType> applicator,
      final EffectTrait<Effect> trait,
      final Function<Event, Effect> projection)
  {
    throw new IllegalStateException("Cannot allocate in a query-only context");
  }

  @Override
  public <Event> void emit(final Event event, final Query<Event, ?> query) {
    throw new IllegalStateException("Cannot update simulation state in a query-only context");
  }

  @Override
  public <Return> String spawn(final TaskFactory<Return> task) {
    throw new IllegalStateException("Cannot schedule tasks in a query-only context");
  }

  @Override
  public String spawn(final String type, final Map<String, SerializedValue> arguments) {
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
