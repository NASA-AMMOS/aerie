package gov.nasa.jpl.aerie.merlin.protocol.model;

public interface EffectTrait<Effect> {
  Effect empty();
  Effect sequentially(Effect prefix, Effect suffix);
  Effect concurrently(Effect left, Effect right);
}
