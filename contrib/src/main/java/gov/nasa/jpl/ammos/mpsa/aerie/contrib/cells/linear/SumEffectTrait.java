package gov.nasa.jpl.ammos.mpsa.aerie.contrib.cells.linear;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.EffectTrait;

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
public final class SumEffectTrait implements EffectTrait<Double> {
  @Override
  public Double empty() {
    return 0.0;
  }

  @Override
  public Double sequentially(final Double prefix, final Double suffix) {
    return prefix + suffix;
  }

  @Override
  public Double concurrently(final Double left, final Double right) {
    return left + right;
  }
}
