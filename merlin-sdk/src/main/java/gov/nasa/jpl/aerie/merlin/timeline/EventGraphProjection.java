package gov.nasa.jpl.aerie.merlin.timeline;

import gov.nasa.jpl.aerie.merlin.timeline.effects.EventGraph;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Projection;

/**
 * The identity projection from event graphs to event graphs.
 *
 * <p>
 * This projection can be used as an alternative to constructing event graphs via the static factory methods defined on
 * {@link EventGraph}. If some function is parametrized over any {@link Projection}, this projection can be provided to
 * obtain a pure syntax tree representation of the effect it would compute.
 * </p>
 *
 * <p>
 * If one wishes to specifically construct an <code>EventGraph</code>, prefer the static factory methods on that class.
 * </p>
 *
 * @param <Event>
 * @see EventGraph
 * @see Projection
 */
public class EventGraphProjection<Event> implements Projection<Event, EventGraph<Event>> {
  @Override
  public final EventGraph<Event> atom(final Event atom) {
    return EventGraph.atom(atom);
  }

  @Override
  public final EventGraph<Event> empty() {
    return EventGraph.empty();
  }

  @Override
  public final EventGraph<Event> sequentially(final EventGraph<Event> prefix, final EventGraph<Event> suffix) {
    return EventGraph.sequentially(prefix, suffix);
  }

  @Override
  public final EventGraph<Event> concurrently(final EventGraph<Event> left, final EventGraph<Event> right) {
    return EventGraph.concurrently(left, right);
  }
}
