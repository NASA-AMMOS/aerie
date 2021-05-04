package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.EffectTrait;

public interface Cell<Effect, Self extends Cell<Effect, Self>> {
  Self duplicate();

  EffectTrait<Effect> effectTrait();

  void react(Effect effect);

  default void step(final Duration duration) {
    // Unless specified, a cell is unaffected by the passage of time.
  }
}
