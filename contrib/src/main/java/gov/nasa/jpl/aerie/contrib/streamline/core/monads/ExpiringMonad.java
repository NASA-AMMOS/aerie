package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;

import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;

/**
 * The {@link Expiring} monad, which demands derived values expire no later than their sources.
 */
public final class ExpiringMonad {
  private ExpiringMonad() {}

  public static <A> Expiring<A> unit(A data) {
    return ExpiringMonadTransformer.unit(IdentityMonad::unit, data);
  }

  public static <A, B> Expiring<B> bind(Expiring<A> a, Function<A, Expiring<B>> f) {
    return ExpiringMonadTransformer.<A, Expiring<A>, B, Expiring<B>>bind(
        IdentityMonad::unit,
        IdentityMonad::bind,
        IdentityMonad::bind,
        a,
        f);
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> Expiring<B> map(Expiring<A> a, Function<A, B> f) {
    return bind(a, f.andThen(ExpiringMonad::unit));
  }

  public static <A, B> Function<Expiring<A>, Expiring<B>> lift(Function<A, B> f) {
    return a -> map(a, f);
  }
}
