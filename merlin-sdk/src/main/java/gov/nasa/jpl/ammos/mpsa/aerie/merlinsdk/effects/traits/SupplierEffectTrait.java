package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

import java.util.function.Supplier;

/**
 * An effect algebra for combining nullary functions whose codomain is another effect algebra.
 *
 * <p>
 * Nullary functions `f : () -> a` are essentially equivalent to the type `a` itself, since each function maps to only
 * a single value. These are useful in strict languages like Java to prevent a costly computation from being performed
 * unless the value is actually deemed necessary. In this way, the <code>SupplierEffectTrait</code> can be considered to
 * lift an "eager" effect algebra into a "lazy" one.
 * </p>
 *
 * @param <Effect> The type of effect to be lifted into a lazy-evaluation context.
 * @see EffectTrait
 * @see Supplier
 */
public class SupplierEffectTrait<Effect> implements EffectTrait<Supplier<Effect>> {
  /**
   * The effect algebra provided at construction time.
   *
   * <p>
   * This is made available to subclasses to avoid a needless extra field in subclasses that need to use the trait.
   * Subclasses <b>must</b> guarantee not to perform operations on this field that observably affect its behavior.
   * </p>
   */
  protected final EffectTrait<Effect> outputTrait;

  /**
   * Construct an effect algebra for nullary functions over a given effect algebra.
   *
   * @param outputTrait An effect algebra on the output type of the nullary functions.
   */
  public SupplierEffectTrait(final EffectTrait<Effect> outputTrait) {
    this.outputTrait = outputTrait;
  }

  @Override
  public final Supplier<Effect> empty() {
    return () -> this.outputTrait.empty();
  }

  @Override
  public final Supplier<Effect> sequentially(final Supplier<Effect> prefix, final Supplier<Effect> suffix) {
    return () -> this.outputTrait.sequentially(prefix.get(), suffix.get());
  }

  @Override
  public final Supplier<Effect> concurrently(final Supplier<Effect> left, final Supplier<Effect> right) {
    return () -> this.outputTrait.concurrently(left.get(), right.get());
  }
}
