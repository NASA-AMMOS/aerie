package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;

import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.*;

/**
 * Monad transformer for {@link ErrorCatching}, M A -> M {@link ErrorCatching}&lt;A&gt;
 *
 * <p>
 *   Bind for this operation does two jobs:
 *   First, if a computation fails by throwing an exception, that exception is caught so as not to crash the simulation.
 *   Second, if a prior computation failed, the exception is passed through and further computations are skipped.
 * </p>
 */
public final class ErrorCatchingMonadTransformer {
  private ErrorCatchingMonadTransformer() {}

  public static <A, MEA> MEA unit(Function<ErrorCatching<A>, MEA> mUnit, A a) {
    return mUnit.apply(success(a));
  }

  public static <A, MEA, B, MEB> MEB bind(
      Function<ErrorCatching<B>, MEB> mUnit,
      BiFunction<MEA, Function<ErrorCatching<A>, MEB>, MEB> mBind,
      MEA mea,
      Function<A, MEB> f) {
    return mBind.apply(mea, eb -> eb.match(
        a -> {
          try {
            return f.apply(a);
          } catch (Throwable e) {
            return mUnit.apply(failure(e));
          }
        },
        e -> mUnit.apply(failure(e))));
  }
}
