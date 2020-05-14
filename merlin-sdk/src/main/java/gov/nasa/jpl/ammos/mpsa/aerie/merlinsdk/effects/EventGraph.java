package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects;

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
public abstract class EventGraph<Event> {
  private EventGraph() {}

  /**
   * Produce a result by recursively visiting each series-parallel component of a graph.
   *
   * <p>
   * The parameters of this method taken together are equivalent to an instance of {@link Projection}.
   * </p>
   *
   * @param trait A visitor to be applied at each non-leaf component of the event graph.
   * @param substitution A visitor to be applied at each atomic event in the event graph.
   * @param <Effect> The result type produced by the visitor.
   * @return The result of the visitor at the root component of the graph.
   */
  public abstract <Effect> Effect evaluate(final EffectTrait<Effect> trait, final Function<Event, Effect> substitution);

  /**
   * Create an empty event graph.
   *
   * @param <Event> The type of event that might be contained by this event graph.
   * @return An empty event graph.
   */
  public static <Event> EventGraph<Event> empty() {
    return new EventGraph<>() {
      @Override
      public <Effect> Effect evaluate(final EffectTrait<Effect> trait, final Function<Event, Effect> substitution) {
        return trait.empty();
      }
    };
  }

  /**
   * Create an event graph consisting of a single atomic event.
   *
   * @param atom An atomic event.
   * @param <Event> The type of the given atomic event.
   * @return An event graph consisting of a single atomic event.
   */
  public static <Event> EventGraph<Event> atom(final Event atom) {
    Objects.requireNonNull(atom);

    return new EventGraph<>() {
      @Override
      public <Effect> Effect evaluate(final EffectTrait<Effect> trait, final Function<Event, Effect> substitution) {
        return substitution.apply(atom);
      }
    };
  }

  /**
   * Create an event graph by combining multiple event graphs of the same type in sequence.
   *
   * @param segments A series of event graphs to combine in sequence.
   * @param <Event> The type of atomic event contained by these graphs.
   * @return An event graph consisting of a sequence of subgraphs.
   */
  @SafeVarargs
  public static <Event> EventGraph<Event> sequentially(final EventGraph<Event>... segments) {
    for (final var segment : segments) Objects.requireNonNull(segment);

    return new EventGraph<>() {
      @Override
      public <Effect> Effect evaluate(final EffectTrait<Effect> trait, final Function<Event, Effect> substitution) {
        var acc = trait.empty();
        for (final var segment : segments) acc = trait.sequentially(acc, segment.evaluate(trait, substitution));
        return acc;
      }
    };
  }

  /**
   * Create an event graph by combining multiple event graphs of the same type in parallel.
   *
   * @param branches A set of event graphs to combine in parallel.
   * @param <Event> The type of atomic event contained by these graphs.
   * @return An event graph consisting of a set of concurrent subgraphs.
   */
  @SafeVarargs
  public static <Event> EventGraph<Event> concurrently(final EventGraph<Event>... branches) {
    for (final var branch : branches) Objects.requireNonNull(branch);

    return new EventGraph<>() {
      @Override
      public <Effect> Effect evaluate(final EffectTrait<Effect> trait, final Function<Event, Effect> substitution) {
        var acc = trait.empty();
        for (final var branch : branches) acc = trait.concurrently(acc, branch.evaluate(trait, substitution));
        return acc;
      }
    };
  }

  /**
   * Produce a result by recursively visiting each series-parallel component of a graph.
   *
   * @param projection A visitor to be applied at each component of the event graph.
   * @param <Effect> The result type produced by the visitor.
   * @return The result of the visitor at the root component of the graph.
   */
  public final <Effect> Effect evaluate(final Projection<Event, Effect> projection) {
    return this.evaluate(projection, projection::atom);
  }

  /**
   * Produce a result by combining atomic events into a single result of the same type.
   *
   * @param trait A visitor to be applied at each non-leaf component of the event graph.
   * @return The result of the visitor at the root component of the graph.
   */
  public final Event evaluate(final EffectTrait<Event> trait) {
    return this.evaluate(trait, x -> x);
  }

  /**
   * Transform atomic events without altering the graph structure.
   *
   * <p>
   * This is a functorial "map" operation.
   * </p>
   *
   * @param transformation A transformation to be applied to each atomic event in the event graph.
   * @param <TargetType> The result type of the given substitution.
   * @return An event graph with the same structure as the input, but potentially different atomic events.
   */
  public final <TargetType> EventGraph<TargetType> map(final Function<Event, TargetType> transformation) {
    Objects.requireNonNull(transformation);

    // Although it would be _correct_ to return a whole new EventGraph with the events substituted, this is neither
    // necessary nor particularly efficient. Any two objects can be considered equivalent so long as every observation
    // that can be made of both of them is indistinguishable. (This concept is called "bisimulation".)
    //
    // Since the only way to "observe" an EventGraph is to evaluate it, we can simply return an object that evaluates in
    // the same way that a fully-reconstructed EventGraph would. This is easy to do: have the evaluate method perform
    // the given transformation before applying the substitution provided at evaluation time. No intermediate EventGraphs
    // need to be constructed.
    //
    // This is called the "Yoneda" transformation in the functional programming literature. We basically get it for free
    // when using visitors / object algebras in Java. See Edward Kmett's blog series on the topic
    // at http://comonad.com/reader/2011/free-monads-for-less/.
    final var that = this;
    return new EventGraph<>() {
      @Override
      public <Effect> Effect evaluate(final EffectTrait<Effect> trait, final Function<TargetType, Effect> substitution) {
        return that.evaluate(trait, transformation.andThen(substitution));
      }
    };
  }

  /**
   * Replace atomic events with subgraphs.
   *
   * <p>
   * This is a monadic "bind" operation, with identity the {@link #atom} constructor.
   * </p>
   *
   * @param transformation A transformation from events to subgraphs, to be applied to each atomic event in the event
   *                       graph.
   * @param <TargetType> The type of event contained by the produced subgraphs.
   * @return An event graph where the original atomic events have been replaced by subgraphs (containing other atomic
   *         events).
   */
  public final <TargetType> EventGraph<TargetType> substitute(final Function<Event, EventGraph<TargetType>> transformation) {
    Objects.requireNonNull(transformation);

    // As with `map`, we don't need to return a fully-reconstructed EventGraph. We can instead return an object that
    // evaluates in the same way that a fully-reconstructed EventGraph would, but with a more efficient representation.
    //
    // In this case, it is sufficient to return a single new object that, when visiting a leaf of the original event
    // graph, applies the provided substitution and then evaluates the resulting subtree, before then propagating that
    // result back up the original graph.
    //
    // This is called the "codensity" transformation in the functional programming literature. We basically get it for
    // free when using visitors / object algebras in Java. See Edward Kmett's blog series on the topic
    // at http://comonad.com/reader/2011/free-monads-for-less/.
    final var that = this;
    return new EventGraph<>() {
      @Override
      public <Effect> Effect evaluate(final EffectTrait<Effect> trait, final Function<TargetType, Effect> substitution) {
        return that.evaluate(trait, v -> transformation.apply(v).evaluate(trait, substitution));
      }
    };
  }
}
