package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events.ApgenEventProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.traits.SettableEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.traits.SumEffectTrait;

import java.util.Objects;

public final class StateEffectEvaluator extends ApgenEventProjection<SettableEffect<Double, Double>> {
    private final String stateName;

    public StateEffectEvaluator(final String stateName) {
        super(new SettableEffectTrait<>(
            new SumEffectTrait(),
            (base, delta) -> base + delta,
            delta -> (delta == 0)));
        this.stateName = stateName;
    }

    @Override
    public final SettableEffect<Double, Double> add(final String stateName, final double amount) {
        if (!Objects.equals(this.stateName, stateName)) return this.unhandled();
        return SettableEffect.add(amount);
    }

    @Override
    public final SettableEffect<Double, Double> set(final String stateName, final double value) {
        if (!Objects.equals(this.stateName, stateName)) return this.unhandled();
        return SettableEffect.setTo(value);
    }
}
