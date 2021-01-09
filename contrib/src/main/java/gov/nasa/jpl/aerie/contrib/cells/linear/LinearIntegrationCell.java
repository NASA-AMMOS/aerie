package gov.nasa.jpl.aerie.contrib.cells.linear;

import gov.nasa.jpl.aerie.merlin.framework.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.timeline.effects.EffectTrait;
import gov.nasa.jpl.aerie.time.Duration;

import static gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics.persistent;

public final class LinearIntegrationCell implements Cell<Double, LinearIntegrationCell> {
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
  public EffectTrait<Double> effectTrait() {
    return new SumEffectTrait();
  }

  @Override
  public void react(final Double delta) {
    this.rate += delta;
  }

  @Override
  public void step(final Duration elapsedTime) {
    // Law: The passage of time shall not alter a valid dynamics.
    this.volume += this.rate * elapsedTime.ratioOver(Duration.SECOND);
  }


  /// Resources
  public DelimitedDynamics<RealDynamics> getVolume() {
    return persistent(RealDynamics.linear(this.volume, this.rate));
  }

  public DelimitedDynamics<RealDynamics> getRate() {
    return persistent(RealDynamics.constant(this.rate));
  }
}
