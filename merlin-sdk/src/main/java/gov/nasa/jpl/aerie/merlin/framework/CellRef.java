package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.timeline.Query;

import java.util.Objects;
import java.util.function.Supplier;

public final class CellRef<Event, CellType> {
  private final Supplier<? extends Context> context;
  private final Query<?, Event, CellType> query;

  public <$Schema> CellRef(
      final Supplier<? extends Context> context,
      final Query<$Schema, Event, CellType> query)
  {
    this.context = Objects.requireNonNull(context);
    this.query = Objects.requireNonNull(query);
  }

  public CellType get() {
    return this.context.get().ask(this.query);
  }

  public void emit(final Event event) {
    this.context.get().emit(event, query);
  }
}
