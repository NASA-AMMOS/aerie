package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.EventGraphTrait;

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
public class EventGraphProjection<Event>
    extends EventGraphTrait<Event>
    implements Projection<Event, EventGraph<Event>>
{
  @Override
  public final EventGraph<Event> atom(final Event atom) {
    return EventGraph.atom(atom);
  }
}
