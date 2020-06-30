package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.AbstractProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

public abstract class IndependentStateEventProjection<Effect>
        extends AbstractProjection<IndependentStateEvent, Effect>
        implements DefaultIndependentStateEventHandler<Effect>
{
    public IndependentStateEventProjection(final EffectTrait<Effect> trait) {
        super(trait);
    }

    @Override
    public final Effect unhandled() {
        return this.empty();
    }

    @Override
    public final Effect atom(final IndependentStateEvent atom) {
        return atom.visit(this);
    }
}
