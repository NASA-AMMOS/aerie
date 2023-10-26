package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import java.util.function.Function;

/**
 * The trivial monad A -> A
 */
public final class IdentityMonad {
  private IdentityMonad() {}

  public static <A> A unit(A a) {
                                return a;
                                         }

  public static <A, B> B bind(A a, Function<A, B> f) {
                                                     return f.apply(a);
    }
}
