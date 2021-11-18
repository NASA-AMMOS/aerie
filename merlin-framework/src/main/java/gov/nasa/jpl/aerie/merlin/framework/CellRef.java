package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Aggregator;

import java.util.function.Function;

public final class CellRef<Event, CellType> {
  public final Query<Event, CellType> query;

  private CellRef(Query<Event, CellType> query) {
    this.query = query;
  }

  public static <Event, Effect, CellType>
  CellRef<Event, CellType> allocate(
      final CellType initialState,
      final Applicator<Effect, CellType> applicator,
      final Aggregator<Effect> aggregator,
      final Function<Event, Effect> eventToEffect
  ) {
    final var query = ModelActions.context.get().allocate(
        initialState,
        applicator,
        aggregator,
        eventToEffect);
    return new CellRef<>(query);
  }

  public static <Effect, CellType>
  CellRef<Effect, CellType> allocate(final CellType initialState, final Applicator<Effect, CellType> applicator, final Aggregator<Effect> aggregator) {
    return allocate(initialState, applicator, aggregator, $ -> $);
  }

  public CellType get() {
    return ModelActions.context.get().ask(this.query);
  }

  public void emit(final Event event) {
    ModelActions.context.get().emit(event, this.query);
  }
}
