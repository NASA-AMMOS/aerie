package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Action;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Transition;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An effect algebra over automata transition functions.
 *
 * Transition functions model a change from a given input state to some output state via some delta effect. We may
 * define sequential composition of transition functions by applying one transition to the state produced from another,
 * and concurrent composition as the composition of the deltas produced by applying both transitions to the same input.
 * This is useful when modeling events as <i>processes</i> that depend on the results from previous effects.
 *
 * @param <State> The type of states to be traversed.
 * @param <Delta> The type of differences between states.
 * @see EffectTrait
 * @see Transition
 * @see Action
 */
public class TransitionEffectTrait<State, Delta> implements EffectTrait<Transition<State, Delta>> {
  protected final EffectTrait<Delta> trait;
  protected final Action<State, Delta> action;

  /**
   * Construct an effect algebra from an effect algebra on transition deltas and an action applying deltas to states.
   *
   * @param trait An effect algebra on the type of deltas between states.
   * @param action An action producing a new state by applying a delta to a given state.
   */
  public TransitionEffectTrait(final EffectTrait<Delta> trait, final Action<State, Delta> action) {
    this.trait = trait;
    this.action = action;
  }

  @Override
  public final Transition<State, Delta> empty() {
    return context -> Pair.of(context, this.trait.empty());
  }

  @Override
  public final Transition<State, Delta> sequentially(
      final Transition<State, Delta> prefix,
      final Transition<State, Delta> suffix
  ) {
    return context -> {
      final var result1 = prefix.step(context);
      final var result2 = suffix.step(result1.getLeft());
      return Pair.of(
          result2.getLeft(),
          this.trait.sequentially(result1.getRight(), result2.getRight()));
    };
  }

  @Override
  public final Transition<State, Delta> concurrently(
      final Transition<State, Delta> left,
      final Transition<State, Delta> right
  ) {
    return context -> {
      final var change1 = left.step(context).getRight();
      final var change2 = right.step(context).getRight();
      final var combined = this.trait.concurrently(change1, change2);
      return Pair.of(
          this.action.apply(context, combined),
          combined);
    };
  }
}
