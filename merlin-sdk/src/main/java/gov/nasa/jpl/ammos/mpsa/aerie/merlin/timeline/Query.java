package gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Projection;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.SimulationTimeline.START_INDEX;

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

    public ModelType getAt(final History<? extends $Schema, ?> history) {
      // SAFETY: This database is based on $Schema, which is tied to this Event type.
      @SuppressWarnings("unchecked")
      final var database = (SimulationTimeline<? extends $Schema, Event>) history.getTimeline();

      // SAFETY: This database is based on $Schema, which associates our ModelType to this index.
      final var cache = database.<ModelType>getQueryCache(this.index);

      // If we already have a model cached for this time point, we can just bail now.
      if (history.getIndex() == START_INDEX) {
        return this.applicator.initial();
      } else if (cache.containsKey(history.getIndex())) {
        return cache.get(history.getIndex());
      }

      // Look for a cached model anytime back to our most recent branch point, if any.
      final var baseIndex = (history.getLastBranchBase() != null) ? history.getLastBranchBase().getIndex() : START_INDEX;
      var previousIndex = history.getIndex();
      while (previousIndex != baseIndex && !cache.containsKey(previousIndex)) {
        previousIndex = database.get(previousIndex).getPrevious();
      }

      final ModelType model;
      if (previousIndex == baseIndex) {
        // We didn't find anything. We'll have to query our previous segment for its model.
        if (history.getLastBranchBase() != null) {
          model = this.applicator.duplicate(this.getAt(history.getLastBranchBase()));
        } else {
          // Well, we don't have a previous segment. Start with the initial model.
          model = this.applicator.initial();
        }
      } else {
        // We found something! Since we have exclusive ownership over our branch segment, we'll remove it from the cache,
        // update it directly, and put it back in the cache at our current time point.
        model = cache.remove(previousIndex);
      }

      // Compute the effects that have occurred since our last update on this branch.
      final var effects = database.evaluate(this.projection, this.projection::atom, previousIndex, history.getIndex());

      // Step this model up to the current point in time.
      for (final var effect : effects) {
        this.applicator.step(model, effect.getKey());
        this.applicator.apply(model, effect.getValue());
      }

      // Cache this model for future queries.
      cache.put(history.getIndex(), model);

      return model;
    }

    public void clearCache(final SimulationTimeline<? extends $Schema, ?> database) {
      database
          .<ModelType>getQueryCache(this.index)
          .clear();
    }
  }
}
