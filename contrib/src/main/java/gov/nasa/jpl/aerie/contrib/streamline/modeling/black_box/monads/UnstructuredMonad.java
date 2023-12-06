package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.function.Function;

/**
 * The {@link Unstructured} monad
 */
public final class UnstructuredMonad {
    private UnstructuredMonad() {}

    public static <A> Unstructured<A> pure(A a) {
        return Unstructured.constant(a);
    }

    public static <A, B> Unstructured<B> apply(Dynamics<A, ?> a, Dynamics<Function<A, B>, ?> f) {
        return new Unstructured<>() {
            @Override
            public B extract() {
                return f.extract().apply(a.extract());
            }

            @Override
            public Unstructured<B> step(Duration t) {
                return apply(a.step(t), f.step(t));
            }
        };
    }

    public static <A> Unstructured<A> join(Dynamics<? extends Dynamics<A, ?>, ?> a) {
        return new Unstructured<>() {
            @Override
            public A extract() {
                return a.extract().extract();
            }

            @Override
            public Unstructured<A> step(Duration t) {
                return join(a.step(t));
            }
        };
    }
}
