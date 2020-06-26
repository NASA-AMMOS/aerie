package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;

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
      final var model = this.applicator.initial();

      final var effects = this.database.evaluate(this.projection, this.projection::atom, START_INDEX, time.getIndex());
      for (final var effect : effects) {
        this.applicator.step(model, effect.getKey());
        this.applicator.apply(model, effect.getValue());
      }

      // TODO: Stash one fork of the model in a cache.
      return model;
    }
  }
}
