package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.AbstractProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

public abstract class ActivityEventProjection<Effect>
        extends AbstractProjection<ActivityEvent, Effect>
        implements DefaultActivityEventHandler<Effect>
{
    public ActivityEventProjection(final EffectTrait<Effect> trait) {
        super(trait);
    }

    @Override
    public final Effect unhandled() {
        return this.empty();
    }

    @Override
    public final Effect atom(final ActivityEvent atom) {
        return atom.visit(this);
    }
}
