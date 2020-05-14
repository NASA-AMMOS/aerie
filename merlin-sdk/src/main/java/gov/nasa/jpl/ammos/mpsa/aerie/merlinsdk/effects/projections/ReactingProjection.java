package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.AbstractProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.EventGraphTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Transition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.TransitionEffectTrait;
import org.apache.commons.lang3.tuple.Pair;

public abstract class ReactingProjection<Var>
    extends AbstractProjection<Var, Transition<EventGraph<Var>, EventGraph<Var>>>
{
  protected abstract EventGraph<Var> react(final EventGraph<Var> context, final Var event);

  public ReactingProjection() {
    super(new TransitionEffectTrait<>(new EventGraphTrait<>(), EventGraph::sequentially));
  }

  @Override
  public final Transition<EventGraph<Var>, EventGraph<Var>> atom(final Var atom) {
    return context -> {
      final var fullContext = EventGraph.sequentially(context, EventGraph.atom(atom));
      final var result = this
          .react(fullContext, atom)  /* Get a set of events in response to this atom. */
          .evaluate(this)            /* Recursively evaluate the events into a transition function. */
          .step(fullContext);        /* Step forward to the point in time after these events. */
      return Pair.of(result.getLeft(), EventGraph.sequentially(EventGraph.atom(atom), result.getRight()));
    };
  }
}
