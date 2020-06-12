package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;

import java.util.function.Function;


/**
 * A specialization of {@link gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.ReactionContext}
 * to an adaptation's own set of projections.
 *
 * <p>
 * This unfortunate class exists because we need to put a <code>ReactionContext</code> in a {@link DynamicCell},
 * and the third type parameter usually also depends on the first type parameter. Java doesn't have higher-kinded types,
 * nor halfway-decent syntax existential types, so we're forced to use a subclass as a type alias to ensure that the
 * relationship between the type parameters is not lost when existentializing within a {@link DynamicCell}.
 * </p>
 *
 * @param <T> The abstract type of the simulation that owns this context.
 */
public final class ReactionContext<T>
    extends gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.ReactionContext<T, Event, Querier<T>>
{
  public static final DynamicCell<ReactionContext<?>> activeContext = DynamicCell.create();

  public ReactionContext(
      final Querier<T> model,
      final Projection<Event, Function<Time<T, Event>, Time<T, Event>>> reactor,
      final Time<T, Event> time
  ) {
    super(model, reactor, time);
  }
}
