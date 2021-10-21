package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;

public interface Applicator<Effect, State> {
  State duplicate(State state);
  void apply(State state, Effect effect);
  void step(State state, Duration duration);
  Optional<Duration> getExpiry(State state);
}
