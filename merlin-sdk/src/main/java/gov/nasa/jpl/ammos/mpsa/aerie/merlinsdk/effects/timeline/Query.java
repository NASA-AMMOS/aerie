package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;

import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline.START_INDEX;

/**
 * A cached query onto a {@link SimulationTimeline}.
 *
 * <p>
 * While {@link Time} objects may be evaluated directly, they have no capacity for caching results and reusing them
 * for evaluations at later time points. Queries, on the other hand, may be registered with a {@link SimulationTimeline},
 * allowing both effects and the entities they act on to be cached for reuse.
 * </p>
 *
 * @param <Scope> The abstract type of the timeline owning the time points to step over.
 * @param <Event> The type of events that may occur over the timeline.
 * @param <Model> The type of entity produced by this query.
 */
public final class Query<Scope, Event, Model> {
  private final InnerQuery<Scope, Event, Model, ?> query;

  /* package-local */
  <Effect> Query(
      final SimulationTimeline<Scope, Event> database,
      final Projection<Event, Effect> projection,
      final Applicator<Effect, Model> applicator
  ) {
    this.query = new InnerQuery<>(database, projection, applicator);
  }

  /**
   * Get the value associated with this query at the given point in time.
   *
   * @param time The time to perform the query at.
   * @return The value associated with this query at the given time.
   */
  public Model getAt(final Time<Scope, Event> time) {
    return this.query.getAt(time);
  }

  private static final class InnerQuery<Scope, Event, Model, Effect> {
    private final SimulationTimeline<Scope, Event> database;
    private final Projection<Event, Effect> projection;
    private final Applicator<Effect, Model> applicator;

    private final Map<Integer, Model> cache = new HashMap<>();

    private InnerQuery(
        final SimulationTimeline<Scope, Event> database,
        final Projection<Event, Effect> projection,
        final Applicator<Effect, Model> applicator
    ) {
      this.database = database;
      this.projection = projection;
      this.applicator = applicator;
    }

    public Model getAt(final Time<Scope, Event> time) {
      // If we already have a model cached for this time point, we can just bail now.
      if (time.getIndex() == START_INDEX) {
        return this.applicator.initial();
      } else if (this.cache.containsKey(time.getIndex())) {
        return this.cache.get(time.getIndex());
      }

      // Look for a cached model anytime back to our most recent branch point, if any.
      final var baseIndex = (time.getLastBranchBase() != null) ? time.getLastBranchBase().getIndex() : START_INDEX;
      var previousIndex = time.getIndex();
      while (previousIndex != baseIndex && !this.cache.containsKey(previousIndex)) {
        previousIndex = this.database.get(previousIndex).getPrevious();
      }

      final Model model;
      if (previousIndex == baseIndex) {
        // We didn't find anything. We'll have to query our previous segment for its model.
        if (time.getLastBranchBase() != null) {
          model = this.applicator.duplicate(this.getAt(time.getLastBranchBase()));
        } else {
          // Well, we don't have a previous segment. Start with the initial model.
          model = this.applicator.initial();
        }
      } else {
        // We found something! Since we have exclusive ownership over our branch segment, we'll remove it from the cache,
        // update it directly, and put it back in the cache at our current time point.
        model = this.cache.remove(previousIndex);
      }

      // Compute the effects that have occurred since our last update on this branch.
      final var effects = this.database.evaluate(this.projection, this.projection::atom, previousIndex, time.getIndex());

      // Step this model up to the current point in time.
      for (final var effect : effects) {
        this.applicator.step(model, effect.getKey());
        this.applicator.apply(model, effect.getValue());
      }

      // Cache this model for future queries.
      this.cache.put(time.getIndex(), model);

      return model;
    }
  }
}
