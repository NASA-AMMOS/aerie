package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;

import java.util.function.Function;

public final class ErrorCatchingMonad {
  private ErrorCatchingMonad() {}

  public static <A> ErrorCatching<A> unit(A a) {
    return ErrorCatchingMonadTransformer.unit(IdentityMonad::unit, a);
  }

  public static <A, B> ErrorCatching<B> bind(ErrorCatching<A> a, Function<A, ErrorCatching<B>> f) {
    return ErrorCatchingMonadTransformer.<A, ErrorCatching<A>, B, ErrorCatching<B>>bind(
        IdentityMonad::unit,
        IdentityMonad::bind,
        a,
        f);
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> ErrorCatching<B> map(ErrorCatching<A> a, Function<A, B> f) {
    return bind(a, f.andThen(ErrorCatchingMonad::unit));
  }

  public static <A, B> Function<ErrorCatching<A>, ErrorCatching<B>> lift(Function<A, B> f) {
    return a -> map(a, f);
  }
}
