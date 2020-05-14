package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A transition function computing a new state from a current state.
 *
 * <p>
 * When a relevant {@code {@link Action}<State,Delta>} exists, an implementor <b>shall</b> guarantee that
 * <code>transition.step(state).getLeft()</code> is observationally indistinguishable from
 * <code>action.apply(state, transition.step(state).getRight()</code>.
 * </p>
 *
 * <p>
 * Although it is formally redundant to provide both a new state and a delta in the presence of such an action, this
 * allows for more efficient stepping between states in situations where the delta is unnecessary, while preserving the
 * delta when no more efficient approach exists. This typically occurs when evaluating an {@link EventGraph} over some
 * initial state, where concurrent branches of the event graph must be stepped from the same starting state, with their
 * accumulated effects merged after the branches re-join.
 * </p>
 *
 * @param <State> The type of states to be traversed.
 * @param <Delta> The type of differences between states.
 */
@FunctionalInterface
public interface Transition<State, Delta> {
  Pair<State, Delta> step(State state);
}
