package gov.nasa.jpl.aerie.merlin.timeline.effects;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.Optional;
import java.util.function.Function;

public final class RecursiveEventGraphEvaluator implements EventGraphEvaluator {
  @Override
  public <EventType, Effect> Optional<Effect> evaluateOptional(
      final EffectTrait<Effect> trait,
      final Function<EventType, Optional<Effect>> substitution,
      final EventGraph<EventType> graph)
  {
    if (graph instanceof EventGraph.Empty) {
      return Optional.empty();
    } else if (graph instanceof EventGraph.Atom<EventType> g) {
      return substitution.apply(g.atom());
    } else if (graph instanceof EventGraph.Sequentially<EventType> g) {
      final var prefix = evaluateOptional(trait, substitution, g.prefix());
      final var suffix = evaluateOptional(trait, substitution, g.suffix());

      if (prefix.isEmpty()) return suffix;
      if (suffix.isEmpty()) return prefix;

      return Optional.of(trait.sequentially(prefix.get(), suffix.get()));
    } else if (graph instanceof EventGraph.Concurrently<EventType> g) {
      final var left = evaluateOptional(trait, substitution, g.left());
      final var right = evaluateOptional(trait, substitution, g.right());

      if (left.isEmpty()) return right;
      if (right.isEmpty()) return left;

      return Optional.of(trait.concurrently(left.get(), right.get()));
    } else {
      throw new IllegalArgumentException();
    }
  }
}
