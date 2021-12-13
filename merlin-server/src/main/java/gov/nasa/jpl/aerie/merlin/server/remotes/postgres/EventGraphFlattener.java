package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public final class EventGraphFlattener {

  static <T> List<Pair<String, T>> flatten(final EventGraph<T> eventGraph) {
    return EventGraphFlattener.flattenHelper(eventGraph, BranchType.SEQUENTIALLY, new LexicographicTag.Empty());
  }

  static <T> EventGraph<T> unflatten(final List<Pair<String, T>> flatGraph) throws InvalidTagException {
    return EventGraphFlattener.unflattenHelper(
        flatGraph.stream().map(pair -> Pair.of(LexicographicTag.of(pair.getLeft()), pair.getRight())).toList(),
        BranchType.SEQUENTIALLY);
  }

  private enum BranchType {
    SEQUENTIALLY,
    CONCURRENTLY;
    private BranchType opposite() {
      if (this.equals(SEQUENTIALLY)) {
        return CONCURRENTLY;
      } else {
        return SEQUENTIALLY;
      }
    }

    private boolean matchesRootNode(final EventGraph<?> eventGraph) {
      if (eventGraph instanceof EventGraph.Sequentially) {
        return this.equals(SEQUENTIALLY);
      }
      if (eventGraph instanceof EventGraph.Concurrently) {
        return this.equals(CONCURRENTLY);
      }
      return false;
    }
  }

  private sealed interface LexicographicTag {
    static LexicographicTag empty() {
      return new LexicographicTag.Empty();
    }

    static LexicographicTag of(final String tag) {
      if (!tag.contains(".")) {
        return LexicographicTag.empty();
      }
      final int periodIndex = tag.indexOf(".", 1);
      if (periodIndex == -1) {
        return new Cons(
            Integer.parseInt(tag.substring(1)),
            LexicographicTag.empty());
      } else {
        return new Cons(
            Integer.parseInt(tag.substring(1, periodIndex)),
            LexicographicTag.of(tag.substring(periodIndex))
        );
      }
    }

    static LexicographicTag append(final LexicographicTag tag, final int count) {
      return new Cons(count, tag);
    }

    String serialize();

    final record Empty() implements LexicographicTag {
      @Override
      public String serialize() {
        return "";
      }
    }
    final record Cons(int first, LexicographicTag rest) implements LexicographicTag {
      @Override
      public String serialize() {
        return this.rest.serialize() + "." + this.first;
      }
    }
  }

  private static <T> List<Pair<String, T>> flattenHelper(
      final EventGraph<T> eventGraph,
      final BranchType branchType,
      final LexicographicTag tag)
  {
    final var result = new ArrayList<Pair<String, T>>();
    if (eventGraph instanceof EventGraph.Empty) {
      return result;
    }
    if (eventGraph instanceof EventGraph.Atom<T> atom) {
      result.add(Pair.of(tag.serialize(), atom.atom()));
      return result;
    }

    final var nextLayer = new ArrayList<EventGraph<T>>();
    absorbSimilarNeighborsIntoRoot(eventGraph, branchType, nextLayer);

    var count = 0;
    for (final var child : nextLayer) {
      count += 1;
      result.addAll(flattenHelper(
          child,
          branchType.opposite(),
          LexicographicTag.append(tag, count)));
    }
    return result;
  }

  /**
   * Given an eventGraph, start from the root, and combine all adjacent nodes that have the same branch type as the root.
   *
   * Writes the children of those nodes to the given result ArrayList.
   *
   * An important property is that none of the returned children will match the provided branchType.

   * @param eventGraph the current sub graph (initially the whole graph)
   * @param branchType the type of the root node of the EventGraph
   * @param result an ArrayList to which children will be appended
   */
  private static <T> void absorbSimilarNeighborsIntoRoot(
      final EventGraph<T> eventGraph,
      final BranchType branchType,
      final ArrayList<EventGraph<T>> result
  ) {
    if (branchType.matchesRootNode(eventGraph)) {
      final var children = getChildren(eventGraph);
      absorbSimilarNeighborsIntoRoot(children.getLeft(), branchType, result);
      absorbSimilarNeighborsIntoRoot(children.getRight(), branchType, result);
    } else {
      result.add(eventGraph);
    }
  }

  private static <T> Pair<EventGraph<T>, EventGraph<T>> getChildren(final EventGraph<T> eventGraph) {
    if (eventGraph instanceof EventGraph.Sequentially<T> sequentially) {
      return Pair.of(sequentially.prefix(), sequentially.suffix());
    } else if (eventGraph instanceof EventGraph.Concurrently<T> concurrently) {
      return Pair.of(concurrently.left(), concurrently.right());
    }
    // As of writing, `getChildren` is only valid to call on a graph that passed `matchesBranchType`
    throw new IllegalArgumentException("getChildren called on an eventGraph that was neither sequential nor concurrent: " + eventGraph);
  }

  private static <T> EventGraph<T> unflattenHelper(
      final List<Pair<LexicographicTag, T>> flatGraph,
      final BranchType branchType
  ) throws InvalidTagException {
    if (flatGraph.size() == 1) {
      return EventGraph.atom(flatGraph.get(0).getRight());
    }
    final var groupedByPrefix = groupByPrefix(flatGraph);
    var graph = EventGraph.<T>empty();
    // Note: we're iterating backwards so that the produced binary tree is right-leaning.
    // This is purely for aesthetics, preferring (a; (b; c)) to the equivalent ((a; b); c)
    for (final var entry : groupedByPrefix.descendingMap().entrySet()) {
      graph = makeNode(branchType, unflattenHelper(entry.getValue(), branchType.opposite()), graph);
    }
    return graph;
  }

  private static <T> TreeMap<Integer, List<Pair<LexicographicTag, T>>>
  groupByPrefix(final List<Pair<LexicographicTag, T>> flatGraph)
  throws InvalidTagException {
    final var pairs = new TreeMap<Integer, List<Pair<LexicographicTag, T>>>();
    for (final var pair : flatGraph) {
      final var tag = pair.getLeft();
      if (!(tag instanceof LexicographicTag.Cons nonEmptyTag)) {
        throw new InvalidTagException("Empty tag encountered before reaching an atom");
      }
      final var key = nonEmptyTag.first();
      pairs.computeIfAbsent(key, x -> new ArrayList<>())
           .add(Pair.of(nonEmptyTag.rest(), pair.getRight()));
    }
    return pairs;
  }

  static class InvalidTagException extends Exception {
    public InvalidTagException(final String s) {
      super(s);
    }
  }

  private static <T> EventGraph<T> makeNode(final BranchType branchType, final EventGraph<T> left, final EventGraph<T> right) {
    if (branchType == BranchType.SEQUENTIALLY) {
      return EventGraph.sequentially(left, right);
    } else {
      return EventGraph.concurrently(left, right);
    }
  }
}
