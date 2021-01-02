package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.timeline.effects.EffectTrait;
import gov.nasa.jpl.aerie.time.Duration;

public interface Cell<Effect, Self extends Cell<Effect, Self>> {
  Self duplicate();

  EffectTrait<Effect> effectTrait();

  void react(Effect effect);

  default void step(final Duration duration) {
    // Unless specified, a model is unaffected by the passage of time.
  }
}
