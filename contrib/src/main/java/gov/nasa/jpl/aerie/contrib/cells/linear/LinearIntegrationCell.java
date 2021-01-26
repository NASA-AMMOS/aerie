package gov.nasa.jpl.aerie.contrib.cells.linear;

import gov.nasa.jpl.aerie.contrib.traits.CommutativeMonoid;
import gov.nasa.jpl.aerie.merlin.framework.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.timeline.effects.EffectTrait;
import gov.nasa.jpl.aerie.time.Duration;

public final class LinearIntegrationCell implements Cell<LinearAccumulationEffect, LinearIntegrationCell> {

  private double volume;
  private double rate;

  public LinearIntegrationCell(final double volume, final double rate) {
    this.volume = volume;
    this.rate = rate;
  }

  @Override
  public LinearIntegrationCell duplicate() {
    return new LinearIntegrationCell(this.volume, this.rate);
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
    if (effect.clearVolume) this.volume = 0;
  }

  @Override
  public void step(final Duration elapsedTime) {
    // Law: The passage of time shall not alter a valid dynamics.
    this.volume += this.rate * elapsedTime.ratioOver(Duration.SECOND);
  }

  public RealDynamics getVolume() {
    return RealDynamics.linear(this.volume, this.rate);
  }

  public RealDynamics getRate() {
    return RealDynamics.constant(this.rate);
  }
}
