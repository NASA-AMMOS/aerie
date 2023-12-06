package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;

import java.util.function.Function;

/**
 * The applicative functor (but not a monad) formed by composing
 * {@link DynamicsMonad} with {@link UnstructuredMonad}.
 */
public final class UnstructuredDynamicsApplicative {
    private UnstructuredDynamicsApplicative() {}

    public static <A> ErrorCatching<Expiring<Unstructured<A>>> pure(A a) {
        return DynamicsMonad.pure(UnstructuredMonad.pure(a));
    }

    public static <A, B> ErrorCatching<Expiring<Unstructured<B>>> apply(ErrorCatching<Expiring<? extends Dynamics<A, ?>>> a, ErrorCatching<Expiring<? extends Dynamics<Function<A, B>, ?>>> f) {
        return DynamicsMonad.apply(a, DynamicsMonad.map(f, UnstructuredMonad::apply));
    }

    // Unstructured<ErrorCatching<Expiring<A>>> has a success status and expiry that can vary with time, as the dynamics are stepped forward.
    // ErrorCatching<Expiring<Unstructured<A>>> has a single success status and expiry, and only the value varies over time.
    // Since the direction required below would lose information, we can't write it in general.
    // This downgrades this structure to an applicative functor, rather than a monad.

    // private static <A> ErrorCatching<Expiring<Unstructured<A>>> distribute(Unstructured<ErrorCatching<Expiring<A>>> a) {
    // }

    // public static <A> ErrorCatching<Expiring<Unstructured<A>>> join(ErrorCatching<Expiring<Unstructured<ErrorCatching<Expiring<Unstructured<A>>>>>> a) {
    //     return DynamicsMonad.map(DynamicsMonad.join(DynamicsMonad.map(a, UnstructuredDynamicsMonad::distribute)), UnstructuredMonad::join);
    // }
}
