package gov.nasa.jpl.aerie.merlin.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;

/**
 * A cached query onto a {@link SimulationTimeline}.
 *
 * <p>
 * While {@link History} objects may be evaluated directly, they have no capacity for caching results and reusing them
 * for evaluations at later time points. Queries, on the other hand, may be registered with a {@link SimulationTimeline},
 * allowing both effects and the entities they act on to be cached for reuse.
 * </p>
 *
 * @param <$Schema> The unique type of the schema against which this query has been defined.
 * @param <CellType> The type of entity produced by this query.
 */
public final class Query<$Schema, Event, CellType> {
  private final Inner<$Schema, Event, ?, CellType> inner;

  /* package-local */
  <Effect> Query(
      final CellType initialState,
      final Applicator<Effect, CellType> applicator,
      final Projection<Event, Effect> projection,
      final int index)
  {
    this.inner = new Inner<>(initialState, applicator, projection, index);
  }

  /* package-local */
  <$Timeline extends $Schema>
  Table<$Timeline, Event, ?, ?> createTable(final SimulationTimeline<$Timeline> database) {
    return this.inner.createTable(database);
  }

  public int getTableIndex() {
    return this.inner.getTableIndex();
  }

  public Optional<Duration> getCurrentExpiry(final CellType state) {
    return this.inner.getCurrentExpiry(state);
  }

  /**
   * Clear the model cache.
   *
   * <p>
   * Calling this may make `getAt` take longer, but it should not affect correctness.
   * </p>
   */
  public void clearCache(final SimulationTimeline<? extends $Schema> database) {
    database.getTable(this.inner.getTableIndex()).clearCache();
  }

  public CellType getInitialValue() {
    return this.inner.getInitialValue();
  }

  private static final class Inner<$Schema, Event, Effect, CellType> {
    private final CellType initialState;
    private final Applicator<Effect, CellType> applicator;
    private final Projection<Event, Effect> projection;
    private final int tableIndex;

    private Inner(
        final CellType initialState,
        final Applicator<Effect, CellType> applicator,
        final Projection<Event, Effect> projection,
        final int tableIndex)
    {
      this.initialState = initialState;
      this.applicator = applicator;
      this.projection = projection;
      this.tableIndex = tableIndex;
    }

    public <$Timeline extends $Schema>
    Table<$Timeline, Event, ?, CellType> createTable(final SimulationTimeline<$Timeline> database) {
      return new Table<>(database, this.initialState, this.applicator, this.projection, this.tableIndex);
    }

    public int getTableIndex() {
      return this.tableIndex;
    }

    public CellType getInitialValue() {
      return this.applicator.duplicate(this.initialState);
    }

    public Optional<Duration> getCurrentExpiry(final CellType state) {
      return this.applicator.getExpiry(state);
    }
  }
}
