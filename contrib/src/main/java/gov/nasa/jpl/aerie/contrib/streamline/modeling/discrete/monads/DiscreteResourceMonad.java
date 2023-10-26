package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class DiscreteResourceMonad {
  private DiscreteResourceMonad() {}

  public static <A> Resource<Discrete<A>> unit(A a) {
    return DiscreteMonadTransformer.unit(ResourceMonad::unit, a);
  }

  public static <A, B> Resource<Discrete<B>> bind(Resource<Discrete<A>> a, Function<A, Resource<Discrete<B>>> f) {
    return DiscreteMonadTransformer.bind(ResourceMonad::bind, a, f);
  }

  // Convenient methods defined in terms of bind and unit:

  public static <A, B> Resource<Discrete<B>> map(Resource<Discrete<A>> a, Function<A, B> f) {
    return bind(a, f.andThen(DiscreteResourceMonad::unit));
  }

  // Map functions with higher arities are defined for discrete resources,
  // because deriving discrete values with many sources is fairly common.

  public static <A, B, C> Resource<Discrete<C>> map(Resource<Discrete<A>> a, Resource<Discrete<B>> b, BiFunction<A, B, C> f) {
    return bind(a, a$ -> map(b, b$ -> f.apply(a$, b$)));
  }

  public static <A, B, C, D> Resource<Discrete<D>> map(Resource<Discrete<A>> a, Resource<Discrete<B>> b, Resource<Discrete<C>> c, TriFunction<A, B, C, D> f) {
    return bind(a, a$ -> map(b, c, (b$, c$) -> f.apply(a$, b$, c$)));
  }

  public static <A, B> Function<Resource<Discrete<A>>, Resource<Discrete<B>>> lift(Function<A, B> f) {
    return a -> map(a, f);
  }

  public static <A, B, C> BiFunction<Resource<Discrete<A>>, Resource<Discrete<B>>, Resource<Discrete<C>>> lift(BiFunction<A, B, C> f) {
    return (a, b) -> map(a, b, f);
  }
}
