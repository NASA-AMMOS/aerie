package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.function.Function;

/**
 * A module for representing {@link EventGraph}s in a textual form.
 *
 * <ul>
 *   <li>The empty graph is rendered as the empty string.</li>
 *   <li>A sequence of graphs is rendered as <code>(x; y)</code>.</li>
 *   <li>A concurrence of graphs is rendered as <code>(x | y)</code>.</li>
 * </ul>
 *
 * <p>
 * Because sequential and concurrent composition are associative (see {@link EffectTrait}), unnecessary parentheses are
 * elided.
 * </p>
 *
 * <p>
 * Because the empty effect is the identity for both kinds of composition, the empty graph is never rendered. For
 * instance, <code>sequentially(empty(), atom("x"))</code> will be rendered as <code>x</code>, as that graph is
 * observationally equivalent to <code>atom("x")</code>.
 * </p>
 *
 * @see EventGraph
 * @see EffectTrait
 */
public final class TreeLogger {
  private TreeLogger() {}

  /**
   * Render an event graph as a string using the event type's natural {@link Object#toString} implementation.
   *
   * @param expression The event graph to render as a string.
   * @return A textual representation of the graph.
   */
  public static String displayTree(final EventGraph<?> expression) {
    return displayTree(expression, Objects::toString);
  }

  /**
   * Render an event graph as a string using the given interpretation of events as strings.
   *
   * @param expression The event graph to render as a string.
   * @param stringifier An interpretation of atomic events as strings.
   * @param <Event> The type of event contained by the event graph.
   * @return A textual representation of the graph.
   */
  public static <Event> String displayTree(final EventGraph<Event> expression, final Function<Event, String> stringifier) {
    return expression
        .map(stringifier)
        .map(x -> Pair.of(EffectOperator.ATOM, x))
        .evaluate(new LogEffectTrait())
        .getRight();
  }

  private enum EffectOperator { EMPTY, SEQ, PAR, ATOM }

  // An effect algebra for computing string representations of transactions.
  private static final class LogEffectTrait implements EffectTrait<Pair<EffectOperator, String>> {
    @Override
    public Pair<EffectOperator, String> empty() {
      return Pair.of(EffectOperator.EMPTY, "");
    }

    @Override
    public Pair<EffectOperator, String> sequentially(
        final Pair<EffectOperator, String> prefix,
        final Pair<EffectOperator, String> suffix
    ) {
      if (prefix.getLeft() == EffectOperator.EMPTY) return suffix;
      if (suffix.getLeft() == EffectOperator.EMPTY) return prefix;

      return Pair.of(EffectOperator.SEQ,
          ((prefix.getLeft() == EffectOperator.PAR) ? ("(" + prefix.getRight() + ")") : prefix.getRight())
          + "; "
          + ((suffix.getLeft() == EffectOperator.PAR) ? ("(" + suffix.getRight() + ")") : suffix.getRight()));
    }

    @Override
    public Pair<EffectOperator, String> concurrently(
        final Pair<EffectOperator, String> left,
        final Pair<EffectOperator, String> right
    ) {
      if (left.getLeft() == EffectOperator.EMPTY) return right;
      if (right.getLeft() == EffectOperator.EMPTY) return left;

      return Pair.of(EffectOperator.PAR,
          ((left.getLeft() == EffectOperator.SEQ) ? ("(" + left.getRight() + ")") : left.getRight())
          + " | "
          + ((right.getLeft() == EffectOperator.SEQ) ? ("(" + right.getRight() + ")") : right.getRight()));
    }
  }
}
