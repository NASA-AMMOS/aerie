package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.model;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.events.IndependentStateEventProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.traits.SettableEffectTrait;

import java.util.Objects;

public final class SettableEffectEvaluator extends IndependentStateEventProjection<SettableEffect<SerializedValue>> {
    private final String stateName;

    public SettableEffectEvaluator(final String stateName) {
        super(new SettableEffectTrait<>());
        this.stateName = stateName;
    }

    @Override
    public final SettableEffect<SerializedValue> add(final String stateName, final double amount) {
        if (!Objects.equals(this.stateName, stateName)) return this.unhandled();
        return SettableEffect.conflict();
    }

    @Override
    public final SettableEffect<SerializedValue> set(final String stateName, final SerializedValue value) {
        if (!Objects.equals(this.stateName, stateName)) return this.unhandled();
        return SettableEffect.setTo(value);
    }
}
