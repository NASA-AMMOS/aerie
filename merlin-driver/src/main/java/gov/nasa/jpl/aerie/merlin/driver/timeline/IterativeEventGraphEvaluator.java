package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.Optional;
import java.util.function.Function;

public final class IterativeEventGraphEvaluator implements EventGraphEvaluator {
  @Override
  public <EventType, Effect>
  Optional<Effect> evaluateOptional(
      final EffectTrait<Effect> trait,
      final Function<EventType, Optional<Effect>> substitution,
      EventGraph<EventType> graph
  ) {
    Continuation<EventType, Effect> andThen = new Continuation.Empty<>();

    while (true) {
      // Drill down the leftmost branches of the par-seq graph until we hit a leaf.
      Optional<Effect> effect$;
      while (true) {
        if (graph instanceof EventGraph.Sequentially<EventType> g) {
          graph = g.prefix();
          andThen = new Continuation.Right<>(Combiner.Sequentially, g.suffix(), andThen);
        } else if (graph instanceof EventGraph.Concurrently<EventType> g) {
          graph = g.left();
          andThen = new Continuation.Right<>(Combiner.Concurrently, g.right(), andThen);
        } else if (graph instanceof EventGraph.Atom<EventType> g) {
          effect$ = substitution.apply(g.atom());
          break;
        } else if (graph instanceof EventGraph.Empty) {
          effect$ = Optional.empty();
          break;
        } else {
          throw new IllegalArgumentException();
        }
      }

      // If this branch didn't produce anything, use the sibling's value instead.
      Effect effect;
      if (effect$.isPresent()) {
        effect = effect$.get();
      } else {
        if (andThen instanceof Continuation.Combine<EventType, Effect> f) {
          andThen = f.andThen();
          effect = f.left();
        } else if (andThen instanceof Continuation.Right<EventType, Effect> f) {
          andThen = f.andThen();
          graph = f.right();
          continue;
        } else if (andThen instanceof Continuation.Empty) {
          return Optional.of(trait.empty());
        } else {
          throw new IllegalArgumentException();
        }
      }

      // Retrace our steps, accumulating the result until we need to drill down again.
      while (true) {
        if (andThen instanceof Continuation.Combine<EventType, Effect> f) {
          andThen = f.andThen();
          effect = switch (f.combiner()) {
            case Sequentially -> trait.sequentially(f.left(), effect);
            case Concurrently -> trait.concurrently(f.left(), effect);
          };
        } else if (andThen instanceof Continuation.Right<EventType, Effect> f) {
          andThen = new Continuation.Combine<>(f.combiner(), effect, f.andThen());
          graph = f.right();
          break;
        } else if (andThen instanceof Continuation.Empty) {
          return Optional.of(effect);
        } else {
          throw new IllegalArgumentException();
        }
      }
    }
  }

  private enum Combiner { Sequentially, Concurrently }

  private sealed interface Continuation<Event, Effect> {
    record Empty<Event, Effect> ()
        implements Continuation<Event, Effect> {}

    record Right<Event, Effect> (Combiner combiner, EventGraph<Event> right, Continuation<Event, Effect> andThen)
        implements Continuation<Event, Effect> {}

    record Combine<Event, Effect> (Combiner combiner, Effect left, Continuation<Event, Effect> andThen)
        implements Continuation<Event, Effect> {}
  }
}
