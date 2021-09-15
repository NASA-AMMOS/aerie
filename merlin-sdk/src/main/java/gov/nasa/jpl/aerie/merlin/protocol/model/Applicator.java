package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public interface Applicator<Effect, State> {
  State initial();
  State duplicate(State state);
  void apply(State state, Effect effect);
  void step(State state, Duration duration);
}
