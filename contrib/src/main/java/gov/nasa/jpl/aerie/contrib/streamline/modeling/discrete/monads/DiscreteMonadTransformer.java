package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Monad transformer for {@link Discrete}, M A -> M ({@link Discrete}&lt;A&gt;)
 */
public final class DiscreteMonadTransformer {
  private DiscreteMonadTransformer() {}

  public static <A, MDA> MDA unit(Function<Discrete<A>, MDA> mUnit, A a) {
    return mUnit.apply(Discrete.discrete(a));
  }

  public static <A, MDA, MDB> MDB bind(BiFunction<MDA, Function<Discrete<A>, MDB>, MDB> mBind, MDA m, Function<A, MDB> f) {
    return mBind.apply(m, d -> f.apply(d.extract()));
  }

  public static <A, MA, MDA> MDA lift(BiFunction<MA, Function<A, Discrete<A>>, MDA> mBind, MA m) {
    return mBind.apply(m, Discrete::discrete);
  }
}
