package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;

import java.util.Objects;
import java.util.function.Supplier;

public final class CellRef<$Schema, Effect, CellType> {
  private final Supplier<? extends Context<$Schema>> context;
  private final Query<$Schema, Effect, CellType> query;

  public CellRef(final Supplier<? extends Context<$Schema>> context, final Query<$Schema, Effect, CellType> query) {
    this.context = Objects.requireNonNull(context);
    this.query = Objects.requireNonNull(query);
  }

  /**
   * Get the state of the referenced cell at a given time.
   *
   * <p>Can be called outside of simulation context.</p>
   *
   * @param now The time from which to get the state of the cell.
   * @return the state of the referenced cell at the given time.
   */
  public CellType getAt(final History<?> now) {
    // SAFETY: All objects accessible within a single adaptation instance have the same brand.
    @SuppressWarnings("unchecked")
    final var brandedNow = (History<? extends $Schema>) now;

    return brandedNow.ask(this.query);
  }


  public CellType get() {
    return this.getAt(this.context.get().now());
  }

  public void emit(final Effect effect) {
    this.context.get().emit(effect, this.query);
  }
}
