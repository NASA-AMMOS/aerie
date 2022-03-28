package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.timeline.EffectExpressionDisplay;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Event;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.driver.timeline.RecursiveEventGraphEvaluator;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Selector;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Topic;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventGraphFlattenerTest {

  public static final List<Topic<Integer>> TOPICS = List.of(new Topic<>(), new Topic<>(), new Topic<>(), new Topic<>(), new Topic<>());

  @Test
  void testFlattenLex() throws EventGraphFlattener.InvalidTagException {
    final var eventGraph = EventGraph.concurrently(EventGraph.atom("a"), EventGraph.sequentially(
        EventGraph.atom("x"),
        EventGraph.sequentially(
            EventGraph.concurrently(
                EventGraph.atom("y"),
                EventGraph.atom("z")
            ),
            EventGraph.atom("w")
        )
    ));
    final var flattenedLex = EventGraphFlattener.flatten(eventGraph);
    final var unFlattenedLex = EventGraphFlattener.unflatten(flattenedLex);

    assertEquals("a | (x; (y | z); w)", eventGraph.toString());
    assertEquals(5, flattenedLex.size());
    assertEquals("a | (x; (y | z); w)", unFlattenedLex.toString());
  }

  @Test
  void testSubgraph() throws EventGraphFlattener.InvalidTagException {
    final var topic1 = new Topic<String>();
    final var topic2 = new Topic<String>();

    final var eventGraph = EventGraph.concurrently(
        EventGraph.atom(Event.create(topic1, "a")),
        EventGraph.sequentially(
            EventGraph.atom(Event.create(topic2, "x")),
            EventGraph.sequentially(
                EventGraph.concurrently(
                    EventGraph.atom(Event.create(topic1, "y")),
                    EventGraph.atom(Event.create(topic2, "z"))
                ),
                EventGraph.atom(Event.create(topic1, "w"))
            )
        ));
    final var recursiveEventGraphEvaluator = new RecursiveEventGraphEvaluator();
    final EventGraph<String> evaluated = recursiveEventGraphEvaluator.evaluate(
        new EventGraph.IdentityTrait<>(),
        new Selector<>(topic1, EventGraph::atom),
        eventGraph).get();
    assertEquals("a | (y; w)", evaluated.toString());

    final List<Pair<String, Event>> flattened = EventGraphFlattener.flatten(eventGraph);
    final var pairs = flattened.stream()
                               .flatMap(pair -> pair.getRight().extract(topic1).stream().map(s -> Pair.of(pair.getLeft(), s)))
                               .toList();
    final var unflattened = EventGraphFlattener.unflatten(pairs);
    assertEquals(evaluated.toString(), unflattened.toString());
  }

  @Property
  @Label("Unflattening a filtered flattened eventgraph should be equivalent to evaluating an EventGraph with a selector")
  public void filteredGraphsAreEquivalent(@ForAll("fanoutWithTopics") final EventGraph<Event> graph, @ForAll("topicsToSelect") final List<Topic<Integer>> topics)
  throws EventGraphFlattener.InvalidTagException
  {
    final EventGraph<Integer> filtered = new RecursiveEventGraphEvaluator().evaluate(
        new EventGraph.IdentityTrait<>(),
        makeSelector(topics),
        graph).orElse(EventGraph.empty());

    final List<Pair<String, Event>> flattened = EventGraphFlattener.flatten(graph);
    final var flattenedAndFiltered = filterPairsByTopics(flattened, topics);
    final var unflattened = EventGraphFlattener.unflatten(flattenedAndFiltered);

    assertEquals(filtered.toString(), unflattened.toString());
  }

  private static List<Pair<String, Integer>> filterPairsByTopics(
      final List<Pair<String, Event>> flattened,
      final List<Topic<Integer>> topics) {
    return flattened
        .stream()
        .flatMap(pair -> topics
            .stream()
            .flatMap(topic -> pair
                .getRight()
                .extract(topic)
                .stream()
                .map(s -> Pair.of(pair.getLeft(), s))))
        .toList();
  }

  @SuppressWarnings("unchecked")
  private static Selector<EventGraph<Integer>> makeSelector(final List<Topic<Integer>> topics) {
    final List<Selector.SelectorRow<Integer, EventGraph<Integer>>> selectorRows =
        topics.stream().map(topic -> new Selector.SelectorRow<>(topic, EventGraph::atom)).toList();

    // SAFETY: The default constructor for the Selector is allows var args.
    // var args is syntactic sugar for an array; however, there is no way
    // to construct a generic array in Java. If this use case becomes more
    // common, we should look into refactoring Selector to take a list, keeping
    // in mind that it is a performance-sensitive part of the code.
    return new Selector<>(selectorRows.toArray(new Selector.SelectorRow[0]));
  }

  static <T> List<T> subset(final List<T> inputList, final int mask) {
    int remainingSetBits = mask;
    final List<T> result = new ArrayList<>();
    while (remainingSetBits != 0) {
      final int index = Integer.numberOfTrailingZeros(remainingSetBits);
      remainingSetBits &= ~(1 << index); // flip the rightmost 1 to 0
      result.add(inputList.get(index));
    }
    return result;
  }

  @Property
  @Label("TaskFrame should faithfully reassemble event graphs")
  public void producedGraphIsCorrect(@ForAll("fanoutWithTopics") final EventGraph<Event> graph)
  throws EventGraphFlattener.InvalidTagException
  {
    final var flattened = EventGraphFlattener.flatten(graph);
    final var result = EventGraphFlattener.unflatten(flattened);

    // Equivalent graphs have equal string representations.
    assertEquals(
        EffectExpressionDisplay.displayGraph(graph),
        EffectExpressionDisplay.displayGraph(result));
  }

  // Generates arbitrary graphs with the "fanout" property: no event has a Concurrently node in its past. */
  // TaskFrame can't generate graphs with the subgraph `(x | y); z`; events cannot be emitted
  // with two branches in their history. We exclude such graphs from generation.
  @Provide("fanoutWithTopics")
  public static Arbitrary<EventGraph<Event>> fanoutGraphs() {

    return eventGraphs(Arbitraries.integers().map(x -> Event.create(TOPICS.get(Math.abs(x) % TOPICS.size()), x))).filter(
        EventGraphFlattenerTest::isFanoutGraph);
  }

  @Provide("topicsToSelect")
  public static Arbitrary<List<Topic<Integer>>> topicsToSelect() {
    return Arbitraries.integers().map(x -> subset(TOPICS, (int) (Math.abs(x) % Math.floor(Math.pow(2, TOPICS.size())))));
  }

  private static <T> Arbitrary<EventGraph<T>> eventGraphs(final Arbitrary<T> atoms) {
    return Arbitraries
        .lazyOf(
            () -> Arbitraries.just(EventGraph.empty()),
            () -> atoms.map(EventGraph::atom),
            () -> eventGraphs(atoms).tuple2().map($ -> EventGraph.concurrently($.get1(), $.get2())),
            () -> eventGraphs(atoms).tuple2().map($ -> EventGraph.sequentially($.get1(), $.get2())));
  }

  private static <T> boolean isFanoutGraph(@ForAll("fanout") final EventGraph<T> graph) {
    if (graph instanceof EventGraph.Empty) {
      return true;
    } else if (graph instanceof EventGraph.Atom) {
      return true;
    } else if (graph instanceof EventGraph.Concurrently<T> g) {
      return isFanoutGraph(g.left()) && isFanoutGraph(g.right());
    } else if (graph instanceof EventGraph.Sequentially<T> g) {
      return !hasConcurrentNode(g.prefix()) && isFanoutGraph(g.suffix());
    } else {
      throw new IllegalArgumentException();
    }
  }

  private static <T> boolean hasConcurrentNode(final EventGraph<T> graph) {
    if (graph instanceof EventGraph.Empty) {
      return false;
    } else if (graph instanceof EventGraph.Atom) {
      return false;
    } else if (graph instanceof EventGraph.Concurrently<T>) {
      return true;
    } else if (graph instanceof EventGraph.Sequentially<T> g) {
      return hasConcurrentNode(g.prefix()) || hasConcurrentNode(g.suffix());
    } else {
      throw new IllegalArgumentException();
    }
  }
}


