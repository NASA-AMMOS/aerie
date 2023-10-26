package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Monad A -> Resource&lt;A&gt;.
 * This is the primary monad for model authors,
 * handling both expiry and "stitching together" of derived resources.
 */
public final class ResourceMonad {
  private ResourceMonad() {}

  public static <A> Resource<A> unit(A a) {
    return ExpiringMonadTransformer.unit(ExpiringToResourceMonad::unit, a);
  }

  public static <A, B> Resource<B> bind(Resource<A> a, Function<A, Resource<B>> f) {
    return ExpiringMonadTransformer.<A, Resource<A>, B, Resource<B>>bind(
        ExpiringToResourceMonad::unit,
        ExpiringToResourceMonad::bind,
        ExpiringToResourceMonad::bind,
        a,
        f);
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> Resource<B> map(Resource<A> a, Function<A, B> f) {
    return bind(a, f.andThen(ResourceMonad::unit));
  }

  public static <A, B, C> Resource<C> map(Resource<A> a, Resource<B> b, BiFunction<A, B, C> f) {
    return bind(a, a$ -> map(b, b$ -> f.apply(a$, b$)));
  }

  public static <A, B, C, D> Resource<D> map(Resource<A> a, Resource<B> b, Resource<C> c, TriFunction<A, B, C, D> f) {
    return bind(a, a$ -> map(b, c, (b$, c$) -> f.apply(a$, b$, c$)));
  }

  public static <A, B, C> Resource<C> bind(Resource<A> a, Resource<B> b, BiFunction<A, B, Resource<C>> f) {
    return bind(a, a$ -> bind(b, b$ -> f.apply(a$, b$)));
  }

  public static <A, B, C, D> Resource<D> bind(Resource<A> a, Resource<B> b, Resource<C> c, TriFunction<A, B, C, Resource<D>> f) {
    return bind(a, a$ -> bind(b, c, (b$, c$) -> f.apply(a$, b$, c$)));
  }

  public static <A, B> Function<Resource<A>, Resource<B>> lift(Function<A, B> f) {
    return a -> map(a, f);
  }

  public static <A, B, C> BiFunction<Resource<A>, Resource<B>, Resource<C>> lift(BiFunction<A, B, C> f) {
    return (a, b) -> map(a, b, f);
  }
}
