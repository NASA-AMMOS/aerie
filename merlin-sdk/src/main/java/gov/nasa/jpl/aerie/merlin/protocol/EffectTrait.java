package gov.nasa.jpl.aerie.merlin.protocol;

public interface EffectTrait<Effect> {
  Effect empty();
  Effect sequentially(Effect prefix, Effect suffix);
  Effect concurrently(Effect left, Effect right);
}
