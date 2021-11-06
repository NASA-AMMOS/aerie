package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A module for representing {@link EffectExpression}s in a textual form.
 *
 * <ul>
 *   <li>The empty expression is rendered as the empty string.</li>
 *   <li>A sequence of expressions is rendered as <code>(x; y)</code>.</li>
 *   <li>A concurrence of expressions is rendered as <code>(x | y)</code>.</li>
 * </ul>
 *
 * <p>
 * Because sequential and concurrent composition are associative (see {@link EffectTrait}), unnecessary parentheses
 * are elided.
 * </p>
 *
 * <p>
 * Because the empty effect is the identity for both kinds of composition, the empty expression is never rendered.
 * For instance, <code>sequentially(empty(), atom("x"))</code> will be rendered as <code>x</code>, as that graph
 * is observationally equivalent to <code>atom("x")</code>.
 * </p>
 *
 * @see EffectExpression
 * @see EffectTrait
 */
public final class EffectExpressionDisplay {
  private EffectExpressionDisplay() {}

  /**
   * Render an event graph as a string using the event type's natural {@link Object#toString} implementation.
   *
   * @param expression The event graph to render as a string.
   * @return A textual representation of the graph.
   */
  public static String displayGraph(final EffectExpression<?> expression) {
    return displayGraph(expression, Objects::toString);
  }

  /**
   * Render an event graph as a string using the given interpretation of events as strings.
   *
   * @param expression The event graph to render as a string.
   * @param stringifier An interpretation of atomic events as strings.
   * @param <Event> The type of event contained by the event graph.
   * @return A textual representation of the graph.
   */
  public static <Event> String displayGraph(final EffectExpression<Event> expression, final Function<Event, String> stringifier) {
    return expression
        .evaluate(new DisplayEffectTrait(), event -> Optional.of($ -> stringifier.apply(event)))
        .map(f -> f.apply(Parent.Unrestricted))
        .orElse("");
  }

  private enum Parent { Unrestricted, Par, Seq }

  // An effect algebra for computing string representations of transactions.
  private static final class DisplayEffectTrait implements EffectTrait<Optional<Function<Parent, String>>> {
    @Override
    public Optional<Function<Parent, String>> empty() {
      return Optional.empty();
    }

    @Override
    public Optional<Function<Parent, String>> sequentially(
        final Optional<Function<Parent, String>> prefix,
        final Optional<Function<Parent, String>> suffix
    ) {
      if (prefix.isEmpty()) return suffix;
      if (suffix.isEmpty()) return prefix;

      return Optional.of(ctx -> {
        final var expr = prefix.get().apply(Parent.Seq) + "; " + suffix.get().apply(Parent.Seq);
        return (ctx == Parent.Par) ? ("(" + expr + ")") : expr;
      });
    }

    @Override
    public Optional<Function<Parent, String>> concurrently(
        final Optional<Function<Parent, String>> left,
        final Optional<Function<Parent, String>> right
    ) {
      if (left.isEmpty()) return right;
      if (right.isEmpty()) return left;

      return Optional.of(ctx -> {
        final var expr = left.get().apply(Parent.Par) + " | " + right.get().apply(Parent.Par);
        return (ctx == Parent.Seq) ? ("(" + expr + ")") : expr;
      });
    }
  }
}
