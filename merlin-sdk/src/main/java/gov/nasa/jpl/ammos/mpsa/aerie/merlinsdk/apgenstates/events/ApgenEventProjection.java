package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.AbstractProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

public abstract class ApgenEventProjection<Effect>
        extends AbstractProjection<ApgenEvent, Effect>
        implements DefaultApgenEventHandler<Effect>
{
    public ApgenEventProjection(final EffectTrait<Effect> trait) {
        super(trait);
    }

    @Override
    public final Effect unhandled() {
        return this.empty();
    }

    @Override
    public final Effect atom(final ApgenEvent atom) {
        return atom.visit(this);
    }
}
