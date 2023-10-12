package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.DynamicsEffect;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;

import java.util.function.Function;

public final class DynamicsMonad {
  private DynamicsMonad() {}

  public static <A> ErrorCatching<Expiring<A>> unit(A a) {
    return ExpiringMonadTransformer.unit(ErrorCatchingMonad::unit, a);
  }

  public static <A, B> ErrorCatching<Expiring<B>> bind(ErrorCatching<Expiring<A>> a, Function<A, ErrorCatching<Expiring<B>>> f) {
    return ExpiringMonadTransformer.<A, ErrorCatching<Expiring<A>>, B, ErrorCatching<Expiring<B>>>bind(
        ErrorCatchingMonad::unit,
        ErrorCatchingMonad::bind,
        ErrorCatchingMonad::bind,
        a,
        f);
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> ErrorCatching<Expiring<B>> map(ErrorCatching<Expiring<A>> a, Function<A, B> f) {
    return bind(a, f.andThen(DynamicsMonad::unit));
  }

  public static <A, B> Function<ErrorCatching<Expiring<A>>, ErrorCatching<Expiring<B>>> lift(Function<A, B> f) {
    return a -> map(a, f);
  }

  // Not fully monadic since we intentionally ignore expiry information, but useful nonetheless.

  public static <A extends Dynamics<?, A>> DynamicsEffect<A> effect(Function<A, A> f) {
    return bindEffect(f.andThen(DynamicsMonad::unit));
  }

  public static <A extends Dynamics<?, A>> DynamicsEffect<A> bindEffect(Function<A, ErrorCatching<Expiring<A>>> f) {
    return ea -> ErrorCatchingMonad.bind(ea, a -> f.apply(a.data()));
  }
}
