package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface Applicator<Effect, Model> {
  Model initial();
  Model duplicate(Model model);
  void apply(Model model, Effect effect);
  void step(Model model, Duration duration);
}
