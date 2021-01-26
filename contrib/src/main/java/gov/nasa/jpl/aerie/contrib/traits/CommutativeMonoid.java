package gov.nasa.jpl.aerie.contrib.traits;

import gov.nasa.jpl.aerie.merlin.timeline.effects.EffectTrait;

import java.util.function.BinaryOperator;

/**
 * An effect algebra for combining floating-point numbers by summing.
 *
 * <p>
 * Since addition of floating-point numbers is commutative and conflict-free, we may interpret both concurrent and
 * sequential composition of integers as addition, with 0.0 the empty value.
 * </p>
 *
 * <p>
 * Technically, IEEE-754 floating-point addition does not satisfy associativity, violating the expectations of the
 * {@link EffectTrait} interface. This has the practical effect of potentially introducing non-deterministic error
 * accumulation. If you need deterministic numerical calculations, prefer a fixed-point representation using integers.
 * </p>
 *
 * @see EffectTrait
 */
public final class CommutativeMonoid<T> implements EffectTrait<T> {
  private final T empty;
  private final BinaryOperator<T> combinator;

  public CommutativeMonoid(final T empty, final BinaryOperator<T> combinator) {
    this.empty = empty;
    this.combinator = combinator;
  }

  @Override
  public T empty() {
    return this.empty;
  }

  @Override
  public T sequentially(final T prefix, final T suffix) {
    return combinator.apply(prefix, suffix);
  }

  @Override
  public T concurrently(final T left, final T right) {
    return combinator.apply(left, right);
  }
}
