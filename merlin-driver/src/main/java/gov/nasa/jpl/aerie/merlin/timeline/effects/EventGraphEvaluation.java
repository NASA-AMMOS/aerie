package gov.nasa.jpl.aerie.merlin.timeline.effects;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.function.Function;

public final class EventGraphEvaluation {
  private EventGraphEvaluation() {}

  public static <Event, Effect> Effect evaluateRecursive(
      final EffectTrait<Effect> trait,
      final Function<Event, Effect> substitution,
      final EventGraph<Event> graph
  ) {
    if (graph instanceof EventGraph.Empty<Event>) {
      return trait.empty();
    } else if (graph instanceof EventGraph.Atom<Event> g) {
      return substitution.apply(g.atom());
    } else if (graph instanceof EventGraph.Sequentially<Event> g) {
      return trait.sequentially(
          evaluateRecursive(trait, substitution, g.prefix()),
          evaluateRecursive(trait, substitution, g.suffix()));
    } else if (graph instanceof EventGraph.Concurrently<Event> g) {
      return trait.concurrently(
          evaluateRecursive(trait, substitution, g.left()),
          evaluateRecursive(trait, substitution, g.right()));
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static <Event, Effect> Effect evaluateIterative(
      final EffectTrait<Effect> trait,
      final Function<Event, Effect> substitution,
      EventGraph<Event> graph
  ) {
    EvaluationContinuation<Event, Effect> andThen = new EvaluationContinuation.Empty<>();

    while (true) {
      // Drill down the leftmost branches of the par-seq graph until we hit a leaf.
      Effect effect;
      while (true) {
        if (graph instanceof EventGraph.Sequentially<Event> g) {
          graph = g.prefix();
          andThen = new EvaluationContinuation.Right<>(Combiner.Sequentially, g.suffix(), andThen);
        } else if (graph instanceof EventGraph.Concurrently<Event> g) {
          graph = g.left();
          andThen = new EvaluationContinuation.Right<>(Combiner.Concurrently, g.right(), andThen);
        } else if (graph instanceof EventGraph.Atom<Event> g) {
          effect = substitution.apply(g.atom());
          break;
        } else if (graph instanceof EventGraph.Empty) {
          effect = trait.empty();
          break;
        } else {
          throw new IllegalArgumentException();
        }
      }

      // Retrace our steps, accumulating the result until we need to drill down again.
      while (true) {
        if (andThen instanceof EvaluationContinuation.Combine<Event, Effect> f) {
          andThen = f.andThen();
          effect = switch (f.combiner()) {
            case Sequentially -> trait.sequentially(f.left(), effect);
            case Concurrently -> trait.concurrently(f.left(), effect);
          };
        } else if (andThen instanceof EvaluationContinuation.Right<Event, Effect> f) {
          andThen = new EvaluationContinuation.Combine<>(f.combiner(), effect, f.andThen());
          graph = f.right();
          break;
        } else if (andThen instanceof EvaluationContinuation.Empty) {
          return effect;
        } else {
          throw new IllegalArgumentException();
        }
      }
    }
  }

  private enum Combiner { Sequentially, Concurrently }

  private sealed interface EvaluationContinuation<Event, Effect> {
    record Empty<Event, Effect> ()
        implements EvaluationContinuation<Event, Effect> {}

    record Right<Event, Effect> (Combiner combiner, EventGraph<Event> right, EvaluationContinuation<Event, Effect> andThen)
        implements EvaluationContinuation<Event, Effect> {}

    record Combine<Event, Effect> (Combiner combiner, Effect left, EvaluationContinuation<Event, Effect> andThen)
        implements EvaluationContinuation<Event, Effect> {}
  }
}
