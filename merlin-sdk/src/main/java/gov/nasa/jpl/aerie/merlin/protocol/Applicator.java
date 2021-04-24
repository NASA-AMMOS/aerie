package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.time.Duration;

public interface Applicator<Effect, State> {
  State initial();
  State duplicate(State state);
  void apply(State state, Effect effect);
  void step(State state, Duration duration);
}
