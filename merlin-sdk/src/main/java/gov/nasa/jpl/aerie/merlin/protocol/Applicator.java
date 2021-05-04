package gov.nasa.jpl.aerie.merlin.protocol;

public interface Applicator<Effect, State> {
  State initial();
  State duplicate(State state);
  void apply(State state, Effect effect);
  void step(State state, Duration duration);
}
