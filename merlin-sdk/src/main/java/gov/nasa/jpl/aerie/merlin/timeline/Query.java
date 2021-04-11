package gov.nasa.jpl.aerie.merlin.timeline;

import gov.nasa.jpl.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Projection;

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
      final Projection<Event, Effect> projection,
      final Applicator<Effect, CellType> applicator,
      final int index)
  {
    this.inner = new Inner<>(projection, applicator, index);
  }

  /* package-local */
  <$Timeline extends $Schema>
  Table<$Timeline, Event, ?, ?> createTable(final SimulationTimeline<$Timeline> database) {
    return this.inner.createTable(database);
  }

  public int getTableIndex() {
    return this.inner.getTableIndex();
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
    private final Projection<Event, Effect> projection;
    private final Applicator<Effect, CellType> applicator;
    private final int tableIndex;

    private Inner(
        final Projection<Event, Effect> projection,
        final Applicator<Effect, CellType> applicator,
        final int tableIndex)
    {
      this.projection = projection;
      this.applicator = applicator;
      this.tableIndex = tableIndex;
    }

    public <$Timeline extends $Schema>
    Table<$Timeline, Event, ?, CellType> createTable(final SimulationTimeline<$Timeline> database) {
      return new Table<>(database, this.projection, this.applicator, this.tableIndex);
    }

    public int getTableIndex() {
      return this.tableIndex;
    }

    public CellType getInitialValue() {
      return this.applicator.initial();
    }
  }
}
