package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;

/**
 * An effect algebra for combining {@link EventGraph} objects sequentially and concurrently.
 *
 * <p>
 * This trait simply proxies to the static factory methods defined on <code>EventGraph</code>. If some function is
 * parametrized over any {@link EffectTrait}, this projection can be provided to obtain a pure syntax tree
 * representation of the effect it would compute.
 * </p>
 *
 * <p>
 * If one wishes to specifically construct an <code>EventGraph</code>, prefer the static factory methods on that class.
 * </p>
 *
 * @param <Event> The type of event contained by event graphs of this type.
 * @see EventGraph
 * @see EffectTrait
 */
public class EventGraphTrait<Event> implements EffectTrait<EventGraph<Event>> {
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
