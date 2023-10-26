package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;

import java.util.function.Function;

public final class ExpiringToResourceMonad {
  private ExpiringToResourceMonad() {}

  public static <A> Resource<A> unit(Expiring<A> a) {
    return ErrorCatchingMonadTransformer.unit(ErrorCatchingToResourceMonad::unit, a);
  }

  public static <A, B> Resource<B> bind(Resource<A> a, Function<Expiring<A>, Resource<B>> f) {
    return ErrorCatchingMonadTransformer.<Expiring<A>, Resource<A>, Expiring<B>, Resource<B>>bind(
        ErrorCatchingToResourceMonad::unit,
        ErrorCatchingToResourceMonad::bind,
        a,
        f);
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> Resource<B> map(Resource<A> a, Function<Expiring<A>, Expiring<B>> f) {
    return bind(a, f.andThen(ExpiringToResourceMonad::unit));
  }
}
