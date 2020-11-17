package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Objects;

public class ModelApplicator<EffectType, ModelType extends Model<EffectType, ModelType>>
    implements Applicator<EffectType, ModelType>
{
  private final ModelType initialState;

  public ModelApplicator(final ModelType initialState) {
    this.initialState = Objects.requireNonNull(initialState);
  }

  @Override
  public ModelType initial() {
    return this.initialState.duplicate();
  }

  @Override
  public ModelType duplicate(final ModelType model) {
    return model.duplicate();
  }

  @Override
  public void apply(final ModelType model, final EffectType effect) {
    model.react(effect);
  }

  @Override
  public void step(final ModelType model, final Duration duration) {
    model.step(duration);
  }
}
