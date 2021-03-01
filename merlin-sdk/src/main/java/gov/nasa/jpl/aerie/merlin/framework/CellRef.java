package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.timeline.Query;

import java.util.Objects;

public final class CellRef<Event, CellType> {
  private final Query<?, Event, CellType> query;

  /*package-local*/
  CellRef(final Query<?, Event, CellType> query) {
    this.query = Objects.requireNonNull(query);
  }

  public CellType get() {
    return ModelActions.context.get().ask(this.query);
  }

  public void emit(final Event event) {
    ModelActions.context.get().emit(event, this.query);
  }
}
