package gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Projection;

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
 * @param <ModelType> The type of entity produced by this query.
 */
public final class Query<$Schema, ModelType> {
  private final Inner<$Schema, ?, ?, ModelType> inner;

  /* package-local */
  <Event, Effect> Query(
      final Projection<Event, Effect> projection,
      final Applicator<Effect, ModelType> applicator,
      final int index)
  {
    this.inner = new Inner<>(projection, applicator, index);
  }

  /* package-local */
  <$Timeline extends $Schema>
  Table<$Timeline, ?, ?, ?> createTable(final SimulationTimeline<$Timeline, ?> database) {
    return this.inner.createTable(database);
  }

  /**
   * Get the value associated with this query at the given point in time.
   *
   * @param history The time to perform the query at.
   * @return The value associated with this query at the given time.
   */
  public ModelType getAt(final History<? extends $Schema, ?> history) {
    return this.inner.getAt(history);
  }

  /**
   * Clear the model cache.
   *
   * <p>
   * Calling this may make `getAt` take longer, but it should not affect correctness.
   * </p>
   */
  public void clearCache(final SimulationTimeline<? extends $Schema, ?> database) {
    this.inner.clearCache(database);
  }

  private static final class Inner<$Schema, Event, Effect, ModelType> {
    private final Projection<Event, Effect> projection;
    private final Applicator<Effect, ModelType> applicator;
    private final int index;

    private Inner(
        final Projection<Event, Effect> projection,
        final Applicator<Effect, ModelType> applicator,
        final int index)
    {
      this.projection = projection;
      this.applicator = applicator;
      this.index = index;
    }

    public <$Timeline extends $Schema>
    Table<$Timeline, Event, ?, ?> createTable(final SimulationTimeline<$Timeline, ?> database) {
      // SAFETY: This database is based on $Schema, which is tied to this Event type.
      @SuppressWarnings("unchecked")
      final var typedDatabase = (SimulationTimeline<$Timeline, Event>) database;

      return new Table<>(typedDatabase, this.projection, this.applicator);
    }

    public <$Timeline extends $Schema> ModelType getAt(final History<$Timeline, ?> history) {
      // SAFETY: This database is based on $Schema, which is tied to this Event type.
      @SuppressWarnings("unchecked")
      final var now = (History<$Timeline, Event>) history;

      final var table = now.getTimeline().<Effect, ModelType>getTable(this.index);

      return table.getAt(now);
    }

    public void clearCache(final SimulationTimeline<? extends $Schema, ?> database) {
      final var table = database.<Effect, ModelType>getTable(this.index);

      table.clearCache();
    }
  }
}
