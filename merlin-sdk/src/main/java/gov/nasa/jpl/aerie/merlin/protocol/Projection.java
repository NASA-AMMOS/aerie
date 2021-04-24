package gov.nasa.jpl.aerie.merlin.protocol;

public interface Projection<Event, Effect> extends EffectTrait<Effect> {
  Effect atom(Event atom);
}
