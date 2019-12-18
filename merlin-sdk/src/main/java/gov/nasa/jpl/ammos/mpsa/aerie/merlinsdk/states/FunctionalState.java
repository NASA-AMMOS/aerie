package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import java.util.function.Supplier;

public class FunctionalState<T> extends DerivedState<T> {
    
    private final Supplier<T> function;

    private FunctionalState(Supplier<T> function) {
        this.function = function;
    }

    public static <T> FunctionalState<T> derivedFrom(Supplier<T> function) {
        return new FunctionalState<T>(function);
    }

    @Override
    public T get() {
        return function.get();
    }
}