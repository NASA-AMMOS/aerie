package gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects;

import gov.nasa.jpl.ammos.mpsa.aerie.time.Duration;

public interface Applicator<Effect, Model> {
  Model initial();
  Model duplicate(Model model);
  void apply(Model model, Effect effect);
  void step(Model model, Duration duration);
}
