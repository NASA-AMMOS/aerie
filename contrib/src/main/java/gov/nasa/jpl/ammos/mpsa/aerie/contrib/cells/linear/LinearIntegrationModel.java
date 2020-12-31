package gov.nasa.jpl.ammos.mpsa.aerie.contrib.cells.linear;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.EffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.time.Duration;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.DelimitedDynamics.persistent;

public final class LinearIntegrationModel implements Model<Double, LinearIntegrationModel> {
  private double _volume;
  private double _rate;

  public LinearIntegrationModel(final double volume, final double rate) {
    this._volume = volume;
    this._rate = rate;
  }

  @Override
  public LinearIntegrationModel duplicate() {
    return new LinearIntegrationModel(this._volume, this._rate);
  }

  @Override
  public EffectTrait<Double> effectTrait() {
    return new SumEffectTrait();
  }

  @Override
  public void react(final Double delta) {
    this._rate += delta;
  }

  @Override
  public void step(final Duration elapsedTime) {
    // Law: The passage of time shall not alter a valid dynamics.
    this._volume += this._rate * elapsedTime.ratioOver(Duration.SECOND);
  }


  /// Resources
  public DelimitedDynamics<RealDynamics> getVolume() {
    return persistent(RealDynamics.linear(this._volume, this._rate));
  }

  public DelimitedDynamics<RealDynamics> getRate() {
    return persistent(RealDynamics.constant(this._rate));
  }
}
