package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.traits;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

import java.util.Objects;

public class SettableEffectTrait<T> implements EffectTrait<SettableEffect<T>> {
    @Override
    public final SettableEffect<T> empty() {
        return SettableEffect.empty();
    }

    @Override
    public final SettableEffect<T> sequentially(final SettableEffect<T> prefix, final SettableEffect<T> suffix) {
        Objects.requireNonNull(prefix);
        Objects.requireNonNull(suffix);

        return (suffix.isEmpty()) ? prefix : suffix;
    }

    @Override
    public final SettableEffect<T> concurrently(final SettableEffect<T> left, final SettableEffect<T> right) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);

        return left.visit(new SettableEffect.Visitor<>() {
            @Override
            public SettableEffect<T> empty() {
                return right;
            }

            @Override
            public SettableEffect<T> setTo(final T leftBase) {
                return right.visit(new SettableEffect.Visitor<>() {
                    @Override
                    public SettableEffect<T> empty() {
                        return left;
                    }

                    @Override
                    public SettableEffect<T> setTo(final T rightBase) {
                        return SettableEffect.conflict();
                    }

                    @Override
                    public SettableEffect<T> conflict() {
                        return right;
                    }
                });
            }

            @Override
            public SettableEffect<T> conflict() {
                return left;
            }
        });
    }
}
