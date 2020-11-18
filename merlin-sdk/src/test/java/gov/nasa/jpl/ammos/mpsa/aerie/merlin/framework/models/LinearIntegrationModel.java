package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.EffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics.persistent;

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
  public static final RealResource<LinearIntegrationModel> volume =
      (model) -> persistent(RealDynamics.linear(model._volume, model._rate));

  public static final RealResource<LinearIntegrationModel> rate =
      (model) -> persistent(RealDynamics.constant(model._rate));
}
