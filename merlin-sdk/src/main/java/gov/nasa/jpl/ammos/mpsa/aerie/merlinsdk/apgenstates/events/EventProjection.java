package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.AbstractProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

public abstract class EventProjection<Effect>
        extends AbstractProjection<Event, Effect>
        implements DefaultEventHandler<Effect>
{
    public EventProjection(final EffectTrait<Effect> trait) {
        super(trait);
    }

    @Override
    public final Effect unhandled() {
        return this.empty();
    }

    @Override
    public final Effect atom(final Event atom) {
        return atom.visit(this);
    }
}
