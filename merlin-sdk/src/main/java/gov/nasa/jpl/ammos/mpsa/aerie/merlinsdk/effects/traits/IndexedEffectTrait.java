package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

import java.util.function.Function;

/**
 * An effect algebra over functions from some index space to another effect algebra.
 *
 * <p>
 * For any pair of functions <code>f, g : A -> B</code>, where <code>B</code> is itself an effect algebra, we can define
 * <code>(f; g)(x) = f(x); g(x)</code> and <code>(f | g)(x) = f(x) | g(x)</code>. This is useful when the same kind of
 * effect may apply to a number of independent resources. If <code>a : A</code> is a resource, then <code>f(a) : B</code>
 * is the effect upon that resource.
 * </p>
 *
 * <p>
 * Functions do not provide a means of querying which indices have non-empty effects. (This is often called the "support"
 * of a function.) For such needs, consider using {@link MapEffectTrait} to accumulate effects of type {@code Map<A, B>},
 * which can be queried for the keys on which they are defined.
 * </p>
 *
 * @param <Index> The type of indices to which to associate effects.
 * @param <Effect> The type of effect to be associated with each index.
 * @see EffectTrait
 * @see Function
 * @see MapEffectTrait
 */
public class IndexedEffectTrait<Index, Effect> implements EffectTrait<Function<Index, Effect>> {
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
   * Construct an effect algebra on functions from an index space to some other effect algebra.
   *
   * <p>
   * Notice that the input type takes no part in effect composition, as it only serves to ensure that effects under one
   * are held independent from effects under another.
   * </p>
   *
   * @param outputTrait An effect algebra on the output type of the functions.
   */
  public IndexedEffectTrait(final EffectTrait<Effect> outputTrait) {
    this.outputTrait = outputTrait;
  }

  @Override
  public final Function<Index, Effect> empty() {
    return x -> this.outputTrait.empty();
  }

  @Override
  public final Function<Index, Effect> sequentially(final Function<Index, Effect> prefix, final Function<Index, Effect> suffix) {
    return x -> this.outputTrait.sequentially(prefix.apply(x), suffix.apply(x));
  }

  @Override
  public final Function<Index, Effect> concurrently(final Function<Index, Effect> left, final Function<Index, Effect> right) {
    return x -> this.outputTrait.concurrently(left.apply(x), right.apply(x));
  }
}
