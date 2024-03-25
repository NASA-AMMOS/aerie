package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

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
  public <State> State ask(final CellId<State> cellId) {
    return this.querier.getState(cellId);
  }

  @Override
  public <Event, Effect, State>
  CellId<State> allocate(
      final State initialState,
      final CellType<Effect, State> cellType,
      final Function<Event, Effect> interpretation,
      final Topic<Event> topic)
  {
    throw new IllegalStateException("Cannot allocate in a query-only context");
  }

  @Override
  public <Event> void emit(final Event event, final Topic<Event> topic) {
    throw new IllegalStateException("Cannot update simulation state in a query-only context");
  }

  @Override
  public void spawn(final TaskFactory<?> task) {
    throw new IllegalStateException("Cannot schedule tasks in a query-only context");
  }

  @Override
  public <Return> void call(final TaskFactory<Return> task) {
    throw new IllegalStateException("Cannot schedule tasks in a query-only context");
  }

  @Override
  public <Return> void tailCall(final TaskFactory<Return> task) {
    throw new IllegalStateException("Cannot schedule tasks in a query-only context");
  }

  @Override
  public void delay(final Duration duration) {
    throw new IllegalStateException("Cannot yield in a query-only context");
  }

  @Override
  public void waitUntil(final Condition condition) {
    throw new IllegalStateException("Cannot yield in a query-only context");
  }
}
