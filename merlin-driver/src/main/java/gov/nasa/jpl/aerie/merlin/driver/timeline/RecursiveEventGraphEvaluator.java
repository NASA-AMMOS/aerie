package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.ArrayList;
import java.util.Optional;

public final class RecursiveEventGraphEvaluator implements EventGraphEvaluator {
  private enum EvalState {DURING, AFTER}  // used to include BEFORE
  private EvalState evaluating = EvalState.DURING;

  /**
   * Compute the effect produced by selected events from an EventGraph as specific by an EffectTrait
   * @param trait specification of how to compute the effect of a partial order of Events
   * @param selector selects what Events are combined
   * @param graph the EventGraph to evaluate
   * @param lastEvent early termination point in the graph; no early termination for a null value or an Event not in the graph
   * @param includeLast whether to include lastEvent in the evaluation
   * @return the Effect resulting from evaluating the EventGraph
   * @param <Effect> the class/interface of the object computed by the EffectTrait
   */
  @Override
  public <Effect> Optional<Effect>
  evaluate(final EffectTrait<Effect> trait, final Selector<Effect> selector, final EventGraph<Event> graph,
           final Event lastEvent, final boolean includeLast) {
    // Make sure we don't bother evaluating after finding the last event -- this shouldn't happen; maybe remove
    if (evaluating == EvalState.AFTER) return Optional.empty();

    // case graph is Atom
    if (graph instanceof EventGraph.Atom<Event> g) {
      if (lastEvent != null && lastEvent.equals(g.atom())) {
        evaluating = EvalState.AFTER;
        if (!includeLast) {
          return Optional.empty();
        }
      }
      return selector.select(trait, g.atom());

    // case graph is Sequentially
    } else if (graph instanceof EventGraph.Sequentially<Event> g) {
      var effect = evaluate(trait, selector, g.prefix(), lastEvent, includeLast);
      while (evaluating != EvalState.AFTER && g.suffix() instanceof EventGraph.Sequentially<Event> rest) {
        effect = sequence(trait, effect, evaluate(trait, selector, rest.prefix(), lastEvent, includeLast));
        g = rest;
      }
      if (evaluating == EvalState.AFTER) return effect;
      return sequence(trait, effect, evaluate(trait, selector, g.suffix(), lastEvent, includeLast));

    // case graph is Concurrently
    } else if (graph instanceof EventGraph.Concurrently<Event> g) {
      var concurrentGraphs = new ArrayList<EventGraph<Event>>();
      var concurrentEffects = new ArrayList<Optional<Effect>>();

      // gather concurrent branches
      concurrentGraphs.add(g.right());
      while (g.left() instanceof EventGraph.Concurrently<Event> rest) {
        concurrentGraphs.add(rest.right());
        g = rest;
      }
      concurrentGraphs.add(g.left());

      // gather effects of each branch, but if found last event, go ahead and return the Effect of that branch
      for (EventGraph<Event> cg : concurrentGraphs) {
        Optional<Effect> effect = evaluate(trait, selector, cg, lastEvent, includeLast);
        // only need the effect from the branch where evaluation terminated
        if (evaluating == EvalState.AFTER) {
          return effect;
        }
        concurrentEffects.add(effect);
      }

      // combine effects across all evaluated branches
      Optional<Effect> effect = Optional.empty();
      for (Optional<Effect> eff : concurrentEffects) {
        effect = merge(trait, eff, effect);
      }
      return effect;

    // case graph is Empty
    } else if (graph instanceof EventGraph.Empty) {
      return Optional.empty();
    } else {
      throw new IllegalArgumentException();
    }
  }

  private static <Effect>
  Optional<Effect> sequence(final EffectTrait<Effect> trait, final Optional<Effect> a, final Optional<Effect> b) {
    if (a.isEmpty()) return b;
    if (b.isEmpty()) return a;

    return Optional.of(trait.sequentially(a.get(), b.get()));
  }

  private static <Effect>
  Optional<Effect> merge(final EffectTrait<Effect> trait, final Optional<Effect> a, final Optional<Effect> b) {
    if (a.isEmpty()) return b;
    if (b.isEmpty()) return a;

    return Optional.of(trait.concurrently(a.get(), b.get()));
  }
}
