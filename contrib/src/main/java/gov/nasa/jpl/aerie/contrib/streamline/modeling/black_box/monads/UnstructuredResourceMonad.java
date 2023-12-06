package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;

import java.util.function.Function;

/**
 * The monad formed by composing
 * {@link ResourceMonad} and {@link UnstructuredMonad}
 */
public final class UnstructuredResourceMonad {
    private UnstructuredResourceMonad() {}

    public static <A> Resource<Unstructured<A>> pure(A a) {
        return ResourceMonad.pure(UnstructuredMonad.pure(a));
    }

    public static <A, B> Resource<Unstructured<B>> apply(Resource<Unstructured<A>> a, Resource<Unstructured<Function<A, B>>> f) {
        return ResourceMonad.apply(a, ResourceMonad.map(f, UnstructuredMonad::apply));
    }

    private static <A> Resource<Unstructured<A>> distribute(Unstructured<Resource<A>> a) {
        return ResourceMonad.pure(UnstructuredMonad.map(a, Resource::getDynamics));
    }

    public static <A> Resource<Unstructured<A>> join(Resource<Unstructured<Resource<Unstructured<A>>>> a) {
        return ResourceMonad.map(ResourceMonad.join(ResourceMonad.map(a, UnstructuredResourceMonad::distribute)), UnstructuredMonad::join);
    }
}
