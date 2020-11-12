package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SumEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics.persistent;

public final class DataModel implements Model<Double, DataModel> {
  private double _volume;
  private double _rate;

  public DataModel(final double volume, final double rate) {
    this._volume = volume;
    this._rate = rate;
  }

  @Override
  public DataModel duplicate() {
    return new DataModel(this._volume, this._rate);
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
  public static final RealResource<DataModel> volume =
      (model) -> persistent(RealDynamics.linear(model._volume, model._rate));

  public static final RealResource<DataModel> rate =
      (model) -> persistent(RealDynamics.constant(model._rate));
}
