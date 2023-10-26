package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.DynamicsEffect;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import java.util.function.Function;

public final class DiscreteDynamicsMonad {
  private DiscreteDynamicsMonad() {}

  public static <A> ErrorCatching<Expiring<Discrete<A>>> unit(A a) {
    return DiscreteMonadTransformer.unit(DynamicsMonad::unit, a);
  }

  public static <A, B> ErrorCatching<Expiring<Discrete<B>>> bind(ErrorCatching<Expiring<Discrete<A>>> a, Function<A, ErrorCatching<Expiring<Discrete<B>>>> f) {
    return DiscreteMonadTransformer.bind(DynamicsMonad::bind, a, f);
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> ErrorCatching<Expiring<Discrete<B>>> map(ErrorCatching<Expiring<Discrete<A>>> a, Function<A, B> f) {
    return bind(a, f.andThen(DiscreteDynamicsMonad::unit));
  }

  public static <A, B> Function<ErrorCatching<Expiring<Discrete<A>>>, ErrorCatching<Expiring<Discrete<B>>>> lift(Function<A, B> f) {
    return a -> map(a, f);
  }

  // Not monadic, strictly speaking, but useful nonetheless.

  public static <A> DynamicsEffect<Discrete<A>> effect(Function<A, A> f) {
      return DynamicsMonad.effect(DiscreteMonad.lift(f));
  }
}
