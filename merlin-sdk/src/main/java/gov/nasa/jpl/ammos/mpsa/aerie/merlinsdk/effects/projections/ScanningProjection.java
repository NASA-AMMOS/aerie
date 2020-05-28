package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.AbstractProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.EventGraphTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Transition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.PairEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.TransitionEffectTrait;
import org.apache.commons.lang3.tuple.Pair;

public class ScanningProjection<Var, Context>
    extends AbstractProjection<Var, Transition<Context, Pair<Context, EventGraph<Pair<Context, Var>>>>>
{
  private final Projection<Var, Context> contextTrait;

  public ScanningProjection(final Projection<Var, Context> contextTrait) {
    super(new TransitionEffectTrait<>(
        new PairEffectTrait<>(contextTrait, new EventGraphTrait<>()),
        (past, change) -> contextTrait.sequentially(past, change.getLeft())));
    this.contextTrait = contextTrait;
  }

  @Override
  public final Transition<Context, Pair<Context, EventGraph<Pair<Context, Var>>>> atom(final Var atom) {
    return past -> {
      final var change = Pair.of(this.contextTrait.atom(atom), EventGraph.atom(Pair.of(past, atom)));
      return Pair.of(this.contextTrait.sequentially(past, change.getLeft()), change);
    };
  }
}
