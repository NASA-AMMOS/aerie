package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;

import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.*;

/**
 * Monad transformer for {@link Expiring}, M A -> M {@link Expiring}&lt;A&gt;
 *
 * <p>
 *   Bind for this monad ensures that results expire no later than the values they're derived from.
 * </p>
 */
public final class ExpiringMonadTransformer {
  private ExpiringMonadTransformer() {}

  public static <A, MEA> MEA unit(Function<Expiring<A>, MEA> mUnit, A a) {
    return mUnit.apply(neverExpiring(a));
  }

  public static <A, MEA, B, MEB> MEB bind(
      Function<Expiring<B>, MEB> mUnit,
      BiFunction<MEA, Function<Expiring<A>, MEB>, MEB> mBind1,
      BiFunction<MEB, Function<Expiring<B>, MEB>, MEB> mBind2,
      MEA mea,
      Function<A, MEB> f) {
    // TODO: for performance, this could take mMap instead of mUnit and mBind2
    return mBind1.apply(mea, ea ->
        mBind2.apply(f.apply(ea.data()), eb ->
            mUnit.apply(expiring(eb.data(), ea.expiry().or(eb.expiry())))));
  }
}
