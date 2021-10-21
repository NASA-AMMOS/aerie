package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;

public final class CellRef<Event, CellType extends Cell<Event, CellType>> {
  private final Query<?, Event, CellType> query;

  public CellRef(final CellType initialState) {
    this.query = ModelActions.context.get().allocate(
        initialState,
        new CellApplicator<>(),
        Projection.from(initialState.effectTrait(), $ -> $));
  }

  public CellType get() {
    return ModelActions.context.get().ask(this.query);
  }

  public void emit(final Event event) {
    ModelActions.context.get().emit(event, this.query);
  }
}
