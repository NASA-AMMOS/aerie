package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import java.util.function.Function;

public final class DiscreteExpiringMonad {
  private DiscreteExpiringMonad() {}

  public static <A> Expiring<Discrete<A>> unit(A a) {
    return DiscreteMonadTransformer.unit(ExpiringMonad::unit, a);
  }

  public static <A, B> Expiring<Discrete<B>> bind(Expiring<Discrete<A>> a, Function<A, Expiring<Discrete<B>>> f) {
    return DiscreteMonadTransformer.bind(ExpiringMonad::bind, a, f);
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> Expiring<Discrete<B>> map(Expiring<Discrete<A>> a, Function<A, B> f) {
    return bind(a, f.andThen(DiscreteExpiringMonad::unit));
  }
}
