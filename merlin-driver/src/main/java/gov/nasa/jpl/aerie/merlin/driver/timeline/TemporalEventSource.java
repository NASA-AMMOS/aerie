package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.driver.engine.SlabList;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.Set;

public record TemporalEventSource(SlabList<TimePoint> points) implements EventSource {
  public TemporalEventSource() {
    this(new SlabList<>());
  }

  public void add(final Duration delta) {
    if (delta.isZero()) return;
    this.points.append(new TimePoint.Delta(delta));
  }

  public void add(final EventGraph<Event> graph) {
    if (graph instanceof EventGraph.Empty) return;
    this.points.append(new TimePoint.Commit(graph, extractTopics(graph)));
  }

  @Override
  public TemporalCursor cursor() {
    return new TemporalCursor();
  }

  public final class TemporalCursor implements Cursor {
    private final SlabList<TimePoint>.SlabIterator iterator = TemporalEventSource.this.points.iterator();

    private TemporalCursor() {}

    @Override
    public boolean hasNext() {
      return this.iterator.hasNext();
    }

    @Override
    public void step(final Cell<?> cell) {
      final var point = this.iterator.next();

      if (point instanceof TimePoint.Delta p) {
        cell.step(p.delta());
      } else if (point instanceof TimePoint.Commit p) {
        if (cell.isInterestedIn(p.topics())) cell.apply(p.events());
      } else {
        throw new IllegalStateException();
      }
    }
  }


  private static Set<Topic<?>> extractTopics(final EventGraph<Event> graph) {
    final var set = new ReferenceOpenHashSet<Topic<?>>();
    extractTopics(graph, set);
    set.trim();
    return set;
  }

  private static void extractTopics(final EventGraph<Event> graph, final Set<Topic<?>> accumulator) {
    if (graph instanceof EventGraph.Empty) {
      // There are no events here!
      return;
    } else if (graph instanceof EventGraph.Atom<Event> g) {
      accumulator.add(g.atom().topic());
    } else if (graph instanceof EventGraph.Sequentially<Event> g) {
      extractTopics(g.prefix(), accumulator);
      extractTopics(g.suffix(), accumulator);
    } else if (graph instanceof EventGraph.Concurrently<Event> g) {
      extractTopics(g.left(), accumulator);
      extractTopics(g.right(), accumulator);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public sealed interface TimePoint {
    record Delta(Duration delta) implements TimePoint {}
    record Commit(EventGraph<Event> events, Set<Topic<?>> topics) implements TimePoint {}
  }
}
