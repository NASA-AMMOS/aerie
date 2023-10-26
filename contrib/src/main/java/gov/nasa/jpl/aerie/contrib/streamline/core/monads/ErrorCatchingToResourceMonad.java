package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;

import java.util.function.Function;

public final class ErrorCatchingToResourceMonad {
  private ErrorCatchingToResourceMonad() {}

  public static <A> Resource<A> unit(ErrorCatching<Expiring<A>> a) {
    return () -> a;
  }

  public static <A, B> Resource<B> bind(
      Resource<A> a,
      Function<ErrorCatching<Expiring<A>>, Resource<B>> f) {
    return () -> f.apply(a.getDynamics()).getDynamics();
  }
}
