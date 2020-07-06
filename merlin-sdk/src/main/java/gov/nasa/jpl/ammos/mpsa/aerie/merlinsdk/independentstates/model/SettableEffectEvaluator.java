package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events.IndependentStateEventProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.traits.SettableEffectTrait;

import java.util.Objects;

public final class SettableEffectEvaluator extends IndependentStateEventProjection<SettableEffect<SerializedParameter>> {
    private final String stateName;

    public SettableEffectEvaluator(final String stateName) {
        super(new SettableEffectTrait<>());
        this.stateName = stateName;
    }

    @Override
    public final SettableEffect<SerializedParameter> add(final String stateName, final double amount) {
        if (!Objects.equals(this.stateName, stateName)) return this.unhandled();
        return SettableEffect.conflict();
    }

    @Override
    public final SettableEffect<SerializedParameter> set(final String stateName, final SerializedParameter value) {
        if (!Objects.equals(this.stateName, stateName)) return this.unhandled();
        return SettableEffect.setTo(value);
    }
}
