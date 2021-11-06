package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.Optional;

public final class RecursiveEventGraphEvaluator implements EventGraphEvaluator {
  @Override
  public <Effect> Optional<Effect>
  evaluate(final EffectTrait<Effect> trait, final Selector<Effect> selector, final EventGraph<Event> graph) {
    if (graph instanceof EventGraph.Empty) {
      return Optional.empty();
    } else if (graph instanceof EventGraph.Atom<Event> g) {
      return selector.select(g.atom());
    } else if (graph instanceof EventGraph.Sequentially<Event> g) {
      final var prefix = evaluate(trait, selector, g.prefix());
      final var suffix = evaluate(trait, selector, g.suffix());

      if (prefix.isEmpty()) return suffix;
      if (suffix.isEmpty()) return prefix;

      return Optional.of(trait.sequentially(prefix.get(), suffix.get()));
    } else if (graph instanceof EventGraph.Concurrently<Event> g) {
      final var left = evaluate(trait, selector, g.left());
      final var right = evaluate(trait, selector, g.right());

      if (left.isEmpty()) return right;
      if (right.isEmpty()) return left;

      return Optional.of(trait.concurrently(left.get(), right.get()));
    } else {
      throw new IllegalArgumentException();
    }
  }
}
