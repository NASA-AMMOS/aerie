package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An effect algebra over pairs of effects.
 *
 * <p>
 * For a pair of values <code>(a, b) : A * B</code>, where <code>A</code> and <code>B</code> are effect algebras, we can
 * define <code>(a, b) | (c, d) = ((a | c), (b | d))</code> and <code>(a, b); (c, d) = ((a; c), (b; d))/code>. In other
 * words, pairs are composed component-wise. This is useful when collecting two distinct kinds of effects simultaneously.
 * </p>
 *
 * @param <X> The type of effect to be held in the first component of the pair.
 * @param <Y> The type of effect to be held in the second component of the pair.
 * @see EffectTrait
 * @see Pair
 */
public class PairEffectTrait<X, Y> implements EffectTrait<Pair<X, Y>> {
  /**
   * The effect algebra of the left component of the pair, provided at construction time.
   *
   * <p>
   * This is made available to subclasses to avoid a needless extra field in subclasses that need to use the trait.
   * Subclasses <b>must</b> guarantee not to perform operations on this field that observably affect its behavior.
   * </p>
   */
  protected final EffectTrait<X> leftTrait;

  /**
   * The effect algebra of the right component of the pair, provided at construction time.
   *
   * <p>
   * This is made available to subclasses to avoid a needless extra field in subclasses that need to use the trait.
   * Subclasses <b>must</b> guarantee not to perform operations on this field that observably affect its behavior.
   * </p>
   */
  protected final EffectTrait<Y> rightTrait;

  /**
   * Construct an effect algebra on pairs of values from two other effect algebras.
   *
   * @param leftTrait An effect algebra on the type of the left component of the pairs.
   * @param rightTrait An effect algebra on the type of the right component of the pairs.
   */
  public PairEffectTrait(final EffectTrait<X> leftTrait, final EffectTrait<Y> rightTrait) {
    this.leftTrait = leftTrait;
    this.rightTrait = rightTrait;
  }

  @Override
  public Pair<X, Y> empty() {
    return Pair.of(leftTrait.empty(), rightTrait.empty());
  }

  @Override
  public Pair<X, Y> sequentially(final Pair<X, Y> prefix, final Pair<X, Y> suffix) {
    return Pair.of(
        leftTrait.sequentially(prefix.getLeft(), suffix.getLeft()),
        rightTrait.sequentially(prefix.getRight(), suffix.getRight()));
  }

  @Override
  public Pair<X, Y> concurrently(final Pair<X, Y> left, final Pair<X, Y> right) {
    return Pair.of(
        leftTrait.concurrently(left.getLeft(), right.getLeft()),
        rightTrait.concurrently(left.getRight(), right.getRight()));
  }
}
