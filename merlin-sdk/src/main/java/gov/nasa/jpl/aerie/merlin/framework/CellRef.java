package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Checkpoint;
import gov.nasa.jpl.aerie.merlin.timeline.Query;

import java.util.Objects;
import java.util.function.Supplier;

public final class CellRef<Effect, CellType> {
  private final Supplier<? extends Context<?>> context;
  private final Query<?, Effect, CellType> query;

  public <$Schema> CellRef(
      final Supplier<? extends Context<$Schema>> context,
      final Query<$Schema, Effect, CellType> query)
  {
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
  public <$Timeline> CellType getAt(final Checkpoint<$Timeline> now) {
    // SAFETY: All objects accessible within a single adaptation instance have the same brand.
    @SuppressWarnings("unchecked")
    final var brandedQuery = (Query<? super $Timeline, Effect, CellType>) this.query;

    return now.ask(brandedQuery);
  }


  public CellType get() {
    return this.get(this.context.get());
  }

  private <$Schema> CellType get(final Context<$Schema> context) {
    // SAFETY: All objects accessible within a single adaptation instance have the same brand.
    @SuppressWarnings("unchecked")
    final var brandedQuery = (Query<? super $Schema, Effect, CellType>) this.query;

    return context.ask(brandedQuery);
  }

  public void emit(final Effect effect) {
    this.emit(this.context.get(), effect);
  }

  private <$Schema> void emit(final Context<$Schema> context, final Effect effect) {
    // SAFETY: CellRef can only be constructed on a context+query pair with matching brands.
    @SuppressWarnings("unchecked")
    final var brandedQuery = (Query<$Schema, Effect, CellType>) this.query;

    context.emit(effect, brandedQuery);
  }
}
