package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.model.Aggregator;

import java.util.Optional;

public final class RecursiveEventGraphEvaluator implements EventGraphEvaluator {
  @Override
  public <Effect> Optional<Effect>
  evaluate(final Aggregator<Effect> aggregator, final Selector<Effect> selector, final EventGraph<Event> graph) {
    if (graph instanceof EventGraph.Atom<Event> g) {
      return selector.select(aggregator, g.atom());
    } else if (graph instanceof EventGraph.Sequentially<Event> g) {
      var effect = evaluate(aggregator, selector, g.prefix());

      while (g.suffix() instanceof EventGraph.Sequentially<Event> rest) {
        effect = sequence(aggregator, effect, evaluate(aggregator, selector, rest.prefix()));
        g = rest;
      }

      return sequence(aggregator, effect, evaluate(aggregator, selector, g.suffix()));
    } else if (graph instanceof EventGraph.Concurrently<Event> g) {
      var effect = evaluate(aggregator, selector, g.right());

      while (g.left() instanceof EventGraph.Concurrently<Event> rest) {
        effect = merge(aggregator, evaluate(aggregator, selector, rest.right()), effect);
        g = rest;
      }

      return merge(aggregator, evaluate(aggregator, selector, g.left()), effect);
    } else if (graph instanceof EventGraph.Empty) {
      return Optional.empty();
    } else {
      throw new IllegalArgumentException();
    }
  }

  private <Effect>
  Optional<Effect> sequence(final Aggregator<Effect> aggregator, final Optional<Effect> a, final Optional<Effect> b) {
    if (a.isEmpty()) return b;
    if (b.isEmpty()) return a;

    return Optional.of(aggregator.sequentially(a.get(), b.get()));
  }

  private <Effect>
  Optional<Effect> merge(final Aggregator<Effect> aggregator, final Optional<Effect> a, final Optional<Effect> b) {
    if (a.isEmpty()) return b;
    if (b.isEmpty()) return a;

    return Optional.of(aggregator.concurrently(a.get(), b.get()));
  }
}
