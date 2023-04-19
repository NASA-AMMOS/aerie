package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.driver.engine.SlabList;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TemporalEventSource implements EventSource, Iterable<TemporalEventSource.TimePoint> {
  public LiveCells liveCells;
  public SlabList<TimePoint> points;
  public TreeMap<Duration, EventGraph<Event>> eventsByTime;  // TODO: REVIEW - Could do binary search on slab list if
                                                             //       a list of time-graph pairs instead of deltas.
  public Map<Topic<?>, TreeMap<Duration, EventGraph<Event>>> eventsByTopic;  // TODO: REVIEW - Could be slab list like eventsByTime
  public Map<TaskId, TreeMap<Duration, EventGraph<Event>>> eventsByTask;
  public Map<EventGraph<Event>, Set<Topic<?>>> topicsForEventGraph;
  public Map<EventGraph<Event>, Set<TaskId>> tasksForEventGraph;
  public Map<Cell<?>, Duration> cellTimes;
  public TemporalEventSource oldEventSource;

  public TemporalEventSource() {
    this(null, new SlabList<>(), new TreeMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
  }

  public TemporalEventSource(
      final LiveCells liveCells,
      final SlabList<TimePoint> points,
      final TreeMap<Duration, EventGraph<Event>> eventsByTime,
      final Map<Topic<?>, TreeMap<Duration, EventGraph<Event>>> eventsByTopic,
      final Map<TaskId, TreeMap<Duration, EventGraph<Event>>> eventsByTask,
      final Map<EventGraph<Event>, Set<Topic<?>>> topicsForEventGraph,
      final Map<EventGraph<Event>, Set<TaskId>> tasksForEventGraph,
      final Map<Cell<?>, Duration> cellTimes,
      final TemporalEventSource oldEventSource)
  {
    this.liveCells = liveCells;
    this.points = points;
    this.eventsByTime = eventsByTime;
    this.eventsByTopic = eventsByTopic;
    this.eventsByTask = eventsByTask;
    this.topicsForEventGraph = topicsForEventGraph;
    this.tasksForEventGraph = tasksForEventGraph;
    this.cellTimes = cellTimes;
    this.oldEventSource = oldEventSource;
  }

  public TemporalEventSource(final LiveCells liveCells) {
    this(liveCells, new SlabList<>(), new TreeMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
    // Assumes the current time is zero, and the cells have not yet been stepped.
    for (final var liveCell : liveCells.getCells()) {
      cellTimes.put(liveCell.get(), Duration.ZERO);
    }
  }

  public void add(final Duration delta) {
    if (delta.isZero()) return;
    this.points.append(new TimePoint.Delta(delta));
  }

  public void add(final EventGraph<Event> graph, final Duration time) {
    final var topics = extractTopics(graph);
    this.points.append(new TimePoint.Commit(graph, topics));
    addIndices(graph, time, topics);
  }

  /**
   * Index the graph by time, topic, and task.
   * @param graph the graph of Events to add
   * @param time the time as a Duration when the events occur
   */
  protected void addIndices(final EventGraph<Event> graph, final Duration time, Set<Topic<?>> topics) {
    eventsByTime.put(time, graph);
    if (topics == null) topics = extractTopics(graph);
    final var tasks = extractTasks(graph);
    topics.forEach(t -> this.eventsByTopic.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, graph));
    tasks.forEach(t -> this.eventsByTask.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, graph));
    // TODO: REVIEW -- do we really need all these maps?
    topicsForEventGraph.computeIfAbsent(graph, $ -> new TreeSet<>()).addAll(topics);  // Tree over Hash for less memory/space
    tasksForEventGraph.computeIfAbsent(graph, $ -> new TreeSet<>()).addAll(tasks);
  }

  @Override
  public Iterator<TimePoint> iterator() {
    return TemporalEventSource.this.points.iterator();
  }

  @Override
  public TemporalCursor cursor() {
    return new TemporalCursor();
  }

  public final class TemporalCursor implements Cursor {
    private final Iterator<TimePoint> iterator;

    TemporalCursor(final Iterator<TimePoint> iterator) {
      this.iterator = iterator;
    }
    private TemporalCursor() {
      this(TemporalEventSource.this.points.iterator());
    }


    /**
     * Step up the Cell for one set of Events (an EventGraph) up to a specified last Event.  Stepping up means to
     * apply Effects from Events up to some point in time.  The EventGraph represents partially time-ordered events.
     * Thus, the Cell may be stepped up to an Event within that partial order.
     * @param cell the Cell to step up
     * @param events the Events that may affect the Cell
     * @param lastEvent a boundary within the graph of Events beyond which Events are not applied
     * @param includeLast whether to apply the Effect of the last Event
     */
    public void stepUp(final Cell<?> cell, final EventGraph<Event> events, final Optional<Event> lastEvent, final boolean includeLast) {
      cell.apply(events, lastEvent, includeLast);
      // TODO: HERE!!!  Check for staleness here with TemporalEventSource.this.oldEventSource

      /*
      try {
        // TODO : What if the lastEvent is in an EventGraph that does not include this cell's topic?  Then we can quit
        //        after reaching the time of that event, but how do we know that time?  Do we need a map of Event to time?
        //        What if we get the topic of lastEvent, and walk through graphs for that topic and look for it?  Hmmmm . . .
        //        Well, we could look at those graphs while stepping up -- not too bad.
        var eventsForTopic = eventsByTopic.get(cell.getTopic());
        var eventsAtTime = eventsForTopic.tailMap(cellTimes.get(cell));
        if (!eventsAtTime.isEmpty()) {
          for (var entry : eventsAtTime.entrySet()) {
            var time = entry.getKey();
            var eventGraph = entry.getValue();
            var delta = time.minus(cellTimes.get(cell));
            if (delta.isPositive()) {
              cell.step(delta);
            } else if (delta.isNegative()) {
              throw new UnsupportedOperationException("Trying to step cell from the past");
            }
            cell.apply(eventGraph, lastEvent, includeLast);
            cellTimes.put(cell, time);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      */
    }

    /**
     * Step up the Cell for one set of Events (an EventGraph) up to a specified last Event.  Stepping up means to
     * apply Effects from Events up to some point in time.
     *
     * @param cell the Cell to step up
     * @param maxTime the time beyond which Events are ignored
     * @param includeMaxTime whether to apply the Events occurring at maxTime
     */
    public void stepUp(final Cell<?> cell, final Duration maxTime, final boolean includeMaxTime) {
      // TODO: HERE!!!  Check for staleness here with TemporalEventSource.this.oldEventSource

      final NavigableMap<Duration, EventGraph<Event>> subTimeline;
      final var currentCellTime = cellTimes.get(cell);
      if (currentCellTime.longerThan(maxTime)) {
        throw new UnsupportedOperationException("Trying to step cell from the past");
      }
      try {
         subTimeline =
            TemporalEventSource.this.eventsByTopic.get(cell.getTopic()).subMap(
                currentCellTime,
                true,
                maxTime,
                includeMaxTime);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      for (final var e : subTimeline.entrySet()) {
        final var p = e.getValue();
        final var delta = e.getKey().minus(currentCellTime);
        if (delta.isPositive()) {
          cell.step(delta);
        } else if (delta.isNegative()) {
          throw new UnsupportedOperationException("Trying to step cell from the past");
        }
        cell.apply(p, null, false);
        cellTimes.put(cell, e.getKey());
      }
    }

    @Override
    public void stepUp(final Cell<?> cell) {
      stepUp(cell, Duration.MAX_VALUE, true);
    }

  }


  public static Set<Topic<?>> extractTopics(final EventGraph<Event> graph) {
    final var set = new ReferenceOpenHashSet<Topic<?>>();
    extractTopics(set, graph);
    set.trim();
    return set;
  }

  public static Set<TaskId> extractTasks(final EventGraph<Event> graph) {
    final var set = new ReferenceOpenHashSet<TaskId>();
    extractTasks(set, graph);
    set.trim();
    return set;
  }

  private static void extractTopics(final Set<Topic<?>> accumulator, EventGraph<Event> graph) {
    while (true) {
      if (graph instanceof EventGraph.Empty) {
        // There are no events here!
        return;
      } else if (graph instanceof EventGraph.Atom<Event> g) {
        accumulator.add(g.atom().topic());
        return;
      } else if (graph instanceof EventGraph.Sequentially<Event> g) {
        extractTopics(accumulator, g.prefix());
        graph = g.suffix();
      } else if (graph instanceof EventGraph.Concurrently<Event> g) {
        extractTopics(accumulator, g.left());
        graph = g.right();
      } else {
        throw new IllegalArgumentException();
      }
    }
  }

  private static void extractTasks(final Set<TaskId> accumulator, EventGraph<Event> graph) {
    while (true) {
      if (graph instanceof EventGraph.Empty) {
        // There are no events here!
        return;
      } else if (graph instanceof EventGraph.Atom<Event> g) {
        accumulator.add(g.atom().provenance());
        return;
      } else if (graph instanceof EventGraph.Sequentially<Event> g) {
        extractTasks(accumulator, g.prefix());
        graph = g.suffix();
      } else if (graph instanceof EventGraph.Concurrently<Event> g) {
        extractTasks(accumulator, g.left());
        graph = g.right();
      } else {
        throw new IllegalArgumentException();
      }
    }
  }

  public sealed interface TimePoint {
    record Delta(Duration delta) implements TimePoint {}
    record Commit(EventGraph<Event> events, Set<Topic<?>> topics) implements TimePoint {}
  }
}
