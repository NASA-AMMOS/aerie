package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;

public interface Applicator<Effect, State> {
  State initial();
  State duplicate(State state);
  void apply(State state, Effect effect);
  void step(State state, Duration duration);

  /** Get the (positive) amount of time that this cell is valid for, or empty if it's valid forever. */
  default Optional<Duration> getExpiry(State state) {
    return Optional.empty();
  }
}
