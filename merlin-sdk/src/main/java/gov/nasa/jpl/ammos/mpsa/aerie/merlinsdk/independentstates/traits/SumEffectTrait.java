package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.traits;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

public final class SumEffectTrait implements EffectTrait<Double> {
    @Override
    public Double empty() {
        return 0.0;
    }

    @Override
    public Double sequentially(final Double prefix, final Double suffix) {
        return prefix + suffix;
    }

    @Override
    public Double concurrently(final Double left, final Double right) {
        return left + right;
    }
}
