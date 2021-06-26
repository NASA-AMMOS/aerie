package gov.nasa.jpl.aerie.merlin.protocol.model;

public interface Projection<Event, Effect> extends EffectTrait<Effect> {
  Effect atom(Event atom);
}
