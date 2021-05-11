package gov.nasa.jpl.aerie.contrib.cells.linear;

import gov.nasa.jpl.aerie.contrib.traits.CommutativeMonoid;
import gov.nasa.jpl.aerie.merlin.framework.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;

public final class LinearIntegrationCell implements Cell<LinearAccumulationEffect, LinearIntegrationCell> {
  public final static double defaultZeroTolerance = 1E-7;

  // We split `initialVolume` from `accumulatedVolume` to avoid loss of floating-point precision.
  // The rate is usually smaller than the volume by some orders of magnitude,
  // so accumulated deltas will usually be closer to each other in magnitude than to the current volume.
  private double initialVolume;
  private double accumulatedVolume;
  private double rate;
  private final double zeroTolerance;

  public LinearIntegrationCell(final double initialVolume, final double rate, final double accumulatedVolume, final double zeroTolerance) {
    this.initialVolume = initialVolume;
    this.accumulatedVolume = accumulatedVolume;
    this.rate = rate;
    this.zeroTolerance = zeroTolerance;
  }

  public LinearIntegrationCell(final double initialVolume, final double rate, final double accumulatedVolume) {
    this(initialVolume, rate, accumulatedVolume, defaultZeroTolerance);
  }

  public LinearIntegrationCell(final double initialVolume, final double rate) {
    this(initialVolume, rate, 0.0);
  }

  @Override
  public LinearIntegrationCell duplicate() {
    return new LinearIntegrationCell(this.initialVolume, this.rate, this.accumulatedVolume);
  }

  @Override
  public EffectTrait<LinearAccumulationEffect> effectTrait() {
    return new CommutativeMonoid<>(
      LinearAccumulationEffect.empty(),
      (left, right) -> new LinearAccumulationEffect(left.deltaRate + right.deltaRate, left.clearVolume || right.clearVolume));
  }

  @Override
  public void react(final LinearAccumulationEffect effect) {
    this.rate += effect.deltaRate;
    if (Math.abs(this.rate) < this.zeroTolerance) this.rate = 0.0;
    if (effect.clearVolume) {
      this.initialVolume = 0;
      this.accumulatedVolume = 0;
    }
  }

  @Override
  public void step(final Duration elapsedTime) {
    // Law: The passage of time shall not alter a valid dynamics.
    this.accumulatedVolume += this.rate * elapsedTime.ratioOver(Duration.SECOND);
  }

  public RealDynamics getVolume() {
    return RealDynamics.linear(this.initialVolume + this.accumulatedVolume, this.rate);
  }

  public RealDynamics getRate() {
    return RealDynamics.constant(this.rate);
  }
}
