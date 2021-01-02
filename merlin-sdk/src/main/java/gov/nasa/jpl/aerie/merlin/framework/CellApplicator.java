package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Objects;

public class CellApplicator<EffectType, ModelType extends Cell<EffectType, ModelType>>
    implements Applicator<EffectType, ModelType>
{
  private final ModelType initialState;

  public CellApplicator(final ModelType initialState) {
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
