package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events.EventProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.traits.SettableEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.traits.SumEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.MapEffectTrait;

import java.util.Map;

public final class StateEffectEvaluator extends EventProjection<Map<String, SettableEffect<Double, Double>>> {
    public StateEffectEvaluator() {
        super(new MapEffectTrait<>(
                new SettableEffectTrait<>(
                        new SumEffectTrait(),
                        (base, delta) -> base + delta,
                        delta -> (delta == 0))));
    }

    @Override
    public final Map<String, SettableEffect<Double, Double>> add(final String stateName, final double amount) {
        return Map.of(stateName, SettableEffect.add(amount));
    }

    @Override
    public final Map<String, SettableEffect<Double, Double>> set(final String stateName, final double value) {
        return Map.of(stateName, SettableEffect.setTo(value));
    }


}
