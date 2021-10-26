package gov.nasa.jpl.aerie.merlin.timeline.effects;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * An immutable tree-representation of a graph of sequentially- and concurrently-composed events.
 *
 * <p>
 * An event graph is a <a href="https://en.wikipedia.org/wiki/Series-parallel_graph">series-parallel graph</a>
 * whose edges represent atomic events. Event graphs may be composed sequentially (in series) or concurrently (in
 * parallel).
 * </p>
 *
 * <p>
 * As with many recursive tree-like structures, an event graph is utilized by accepting a {@link Projection} object and
 * traversing the series-parallel structure recursively. A projection provides methods for each type of node in the tree
 * representation (empty, atomic event, sequential composition, and parallel composition). For each node, the projection
 * computes a result that will be provided to the same projection at the parent node. The result of the traversal is the
 * value computed by the projection at the root node.
 * </p>
 *
 * <p>
 * Different domains may interpret each event differently, and so evaluate the same event graph under different
 * projections. An event may have no particular effect in one domain, while being critically important to another
 * domain.
 * </p>
 *
 * @param <Event> The type of event to be stored in the graph structure.
 * @see Projection
 * @see EffectTrait
 */
public sealed interface EventGraph<Event> extends EffectExpression<Event> {
  /** Use {@link EventGraph#empty()} instead of instantiating this class directly. */
  record Empty<Event>() implements EventGraph<Event> {
    // The behavior of the empty graph is independent of the parameterized Event type,
    // so we cache a single instance and re-use it for all Event types.
    private static final EventGraph<?> EMPTY = new Empty<>();

    @Override
    public String toString() {
      return EffectExpressionDisplay.displayGraph(this);
    }
  }

  /** Use {@link EventGraph#atom(Event)} instead of instantiating this class directly. */
  record Atom<Event>(Event atom) implements EventGraph<Event> {
    @Override
    public String toString() {
      return EffectExpressionDisplay.displayGraph(this);
    }
  }

  /** Use {@link EventGraph#sequentially(EventGraph[])}} instead of instantiating this class directly. */
  record Sequentially<Event>(EventGraph<Event> prefix, EventGraph<Event> suffix) implements EventGraph<Event> {
    @Override
    public String toString() {
      return EffectExpressionDisplay.displayGraph(this);
    }
  }

  /** Use {@link EventGraph#concurrently(EventGraph[])}} instead of instantiating this class directly. */
  record Concurrently<Event>(EventGraph<Event> left, EventGraph<Event> right) implements EventGraph<Event> {
    @Override
    public String toString() {
      return EffectExpressionDisplay.displayGraph(this);
    }
  }

  default <Effect> Effect evaluate(final EffectTrait<Effect> trait, final Function<Event, Effect> substitution) {
    return EventGraphEvaluation.evaluateIterative(trait, substitution, this);
  }

  /**
   * Create an empty event graph.
   *
   * @param <Event> The type of event that might be contained by this event graph.
   * @return An empty event graph.
   */
  @SuppressWarnings("unchecked")
  static <Event> EventGraph<Event> empty() {
    return (EventGraph<Event>) Empty.EMPTY;
  }

  /**
   * Create an event graph consisting of a single atomic event.
   *
   * @param atom An atomic event.
   * @param <Event> The type of the given atomic event.
   * @return An event graph consisting of a single atomic event.
   */
  static <Event> EventGraph<Event> atom(final Event atom) {
    return new Atom<>(Objects.requireNonNull(atom));
  }

  /**
   * Create an event graph by combining multiple event graphs of the same type in sequence.
   *
   * @param prefix The first event graph to apply.
   * @param suffix The second event graph to apply.
   * @param <Event> The type of atomic event contained by these graphs.
   * @return An event graph consisting of a sequence of subgraphs.
   */
  static <Event> EventGraph<Event> sequentially(final EventGraph<Event> prefix, final EventGraph<Event> suffix) {
    if (prefix instanceof Empty) return suffix;
    if (suffix instanceof Empty) return prefix;

    return new Sequentially<>(Objects.requireNonNull(prefix), Objects.requireNonNull(suffix));
  }

  /**
   * Create an event graph by combining multiple event graphs of the same type in parallel.
   *
   * @param left An event graph to apply concurrently.
   * @param right An event graph to apply concurrently.
   * @param <Event> The type of atomic event contained by these graphs.
   * @return An event graph consisting of a set of concurrent subgraphs.
   */
  static <Event> EventGraph<Event> concurrently(final EventGraph<Event> left, final EventGraph<Event> right) {
    if (left instanceof Empty) return right;
    if (right instanceof Empty) return left;

    return new Sequentially<>(Objects.requireNonNull(left), Objects.requireNonNull(right));
  }

  /**
   * Create an event graph by combining multiple event graphs of the same type in sequence.
   *
   * @param segments A series of event graphs to combine in sequence.
   * @param <Event> The type of atomic event contained by these graphs.
   * @return An event graph consisting of a sequence of subgraphs.
   */
  static <Event> EventGraph<Event> sequentially(final List<EventGraph<Event>> segments) {
    var acc = EventGraph.<Event>empty();
    for (final var segment : segments) acc = sequentially(acc, segment);
    return acc;
  }

  /**
   * Create an event graph by combining multiple event graphs of the same type in parallel.
   *
   * @param branches A set of event graphs to combine in parallel.
   * @param <Event> The type of atomic event contained by these graphs.
   * @return An event graph consisting of a set of concurrent subgraphs.
   */
  static <Event> EventGraph<Event> concurrently(final Collection<EventGraph<Event>> branches) {
    var acc = EventGraph.<Event>empty();
    for (final var branch : branches) acc = concurrently(acc, branch);
    return acc;
  }

  /**
   * Create an event graph by combining multiple event graphs of the same type in sequence.
   *
   * @param segments A series of event graphs to combine in sequence.
   * @param <Event> The type of atomic event contained by these graphs.
   * @return An event graph consisting of a sequence of subgraphs.
   */
  @SafeVarargs
  static <Event> EventGraph<Event> sequentially(final EventGraph<Event>... segments) {
    return sequentially(Arrays.asList(segments));
  }

  /**
   * Create an event graph by combining multiple event graphs of the same type in parallel.
   *
   * @param branches A set of event graphs to combine in parallel.
   * @param <Event> The type of atomic event contained by these graphs.
   * @return An event graph consisting of a set of concurrent subgraphs.
   */
  @SafeVarargs
  static <Event> EventGraph<Event> concurrently(final EventGraph<Event>... branches) {
    return concurrently(Arrays.asList(branches));
  }
}
