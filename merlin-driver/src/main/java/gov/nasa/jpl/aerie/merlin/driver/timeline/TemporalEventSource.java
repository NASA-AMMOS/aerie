package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.engine.SlabList;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public class TemporalEventSource implements EventSource, Iterable<TemporalEventSource.TimePoint> {
  public LiveCells liveCells;
  private final MissionModel<?> missionModel;
  public SlabList<TimePoint> points = new SlabList<>();  // This is not used for stepping Cells anymore.  Remove?
  public TreeMap<Duration, TimePoint.Commit> commitsByTime = new TreeMap<>();
  public Map<Topic<?>, TreeMap<Duration, EventGraph<Event>>> eventsByTopic = new HashMap<>();
  public Map<TaskId, TreeMap<Duration, EventGraph<Event>>> eventsByTask = new HashMap<>();
  public Map<EventGraph<Event>, Set<Topic<?>>> topicsForEventGraph = new HashMap<>();
  public Map<EventGraph<Event>, Set<TaskId>> tasksForEventGraph = new HashMap<>();
  public Map<EventGraph<Event>, Duration> timeForEventGraph = new HashMap<>();
  protected HashMap<Cell<?>, Duration> cellTimes = new HashMap<>();
  protected HashMap<Cell<?>, Boolean> cellTimeStepped = new HashMap<>();
  public TemporalEventSource oldTemporalEventSource;

  /**
   * cellCache keeps duplicates and old cells that can be reused to more quickly get a past cell value.
   * For example, if a task needs to re-run but starts in the past, we can re-run it from a past point,
   * and successive reads a cell can use a duplicate cached cell stepped up from its initial state.
   */
  private final HashMap<Topic<?>, TreeMap<Duration, Cell<?>>> cellCache = new HashMap<>();

  /** When topics/cells become stale */
  public final Map<Topic<?>, TreeMap<Duration, Boolean>> staleTopics = new HashMap<>();


  public TemporalEventSource() {
    this(null, null, null);
  }

  public TemporalEventSource(
      final LiveCells liveCells,
      final MissionModel<?> missionModel,
      final TemporalEventSource oldTemporalEventSource)
  {
    this.liveCells = liveCells;
    this.missionModel = missionModel;
    this.oldTemporalEventSource = oldTemporalEventSource;
    // Assumes the current time is zero, and the cells have not yet been stepped.
    if (liveCells != null) {
      for (LiveCell<?> liveCell : liveCells.getCells()) {
        final Cell<?> cell = liveCell.get();
        putCellTime(cell, Duration.ZERO, false);
      }
    }
  }

  public TemporalEventSource(LiveCells liveCells) {
    this(liveCells, null, null);
  }

  public void add(final Duration delta) {
    if (delta.isZero()) return;
    this.points.append(new TimePoint.Delta(delta));
  }

  public void add(final EventGraph<Event> graph, Duration time) {
    var topics = extractTopics(graph);
    var commit = new TimePoint.Commit(graph, topics);
    this.points.append(commit);
    addIndices(commit, time, topics);
  }

  /**
   * Index the commit and graph by time, topic, and task.
   * @param commit the commit of Events to add
   * @param time the time as a Duration when the events occur
   */
  protected void addIndices(final TimePoint.Commit commit, Duration time, Set<Topic<?>> topics) {
    commitsByTime.put(time, commit);
    final var finalTopics = topics == null ? extractTopics(commit.events) : topics;
    final var tasks = extractTasks(commit.events);
    topics.forEach(t -> this.eventsByTopic.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, commit.events));
    tasks.forEach(t -> this.eventsByTask.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, commit.events));
    // TODO: REVIEW -- do we really need all these maps?
    topicsForEventGraph.computeIfAbsent(commit.events, $ -> HashSet.newHashSet(finalTopics.size())).addAll(topics);  // Tree over Hash for less memory/space
    tasksForEventGraph.computeIfAbsent(commit.events, $ -> HashSet.newHashSet(tasks.size())).addAll(tasks);
  }

  public void replaceEventGraph(EventGraph<Event> oldG, EventGraph<Event> newG) {
    var newTopics = extractTopics(newG);

    // time
    Duration time = timeForEventGraph.get(oldG);
    timeForEventGraph.remove(oldG);
    timeForEventGraph.put(newG, time);
    var commit = new TimePoint.Commit(newG, newTopics);
    commitsByTime.put(time, commit);

    // task
    tasksForEventGraph.remove(oldG);
    var newTasks = extractTasks(newG);
    tasksForEventGraph.put(newG, newTasks);
    newTasks.forEach(t -> eventsByTask.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, newG));

    // topic
    topicsForEventGraph.remove(oldG);
    topicsForEventGraph.put(newG, newTopics);
    newTopics.forEach(t -> eventsByTopic.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, newG));
  }


  @Override
  public Iterator<TimePoint> iterator() {
      return TemporalEventSource.this.points.iterator();
  }


  public void setTopicStale(Topic<?> topic, Duration offsetTime) {
    staleTopics.computeIfAbsent(topic, $ -> new TreeMap<>()).put(offsetTime, true);
  }

  public void setTopicUnstale(Topic<?> topic, Duration offsetTime) {
    staleTopics.computeIfAbsent(topic, $ -> new TreeMap<>()).put(offsetTime, false);
  }

  /**
   * Determine whether a topic been marked stale at a specified time.
   * @param topic topic to check for staleness
   * @param timeOffset the staleness time
   * @return true if the topic is marked stale at timeOffset
   */
  public boolean isTopicStale(Topic<?> topic, Duration timeOffset) {
    var map = this.staleTopics.get(topic);
    if (map == null) return false;
    final Duration staleTime = map.floorKey(timeOffset);
    return staleTime != null && map.get(staleTime);
  }



  /**
   * Step up the Cell for one set of Events (an EventGraph) up to a specified last Event.  Stepping up means to
   * apply Effects from Events up to some point in time.  The EventGraph represents partially time-ordered events.
   * Thus, the Cell may be stepped up to an Event within that partial order.
   * <p>
   * Staleness is not checked here and must be handled by the caller.
   *
   * @param cell the Cell to step up
   * @param events the Events that may affect the Cell
   * @param lastEvent a boundary within the graph of Events beyond which Events are not applied
   * @param includeLast whether to apply the Effect of the last Event
   */
  public void stepUp(final Cell<?> cell, EventGraph<Event> events, final Event lastEvent, final boolean includeLast) {
    cell.apply(events, lastEvent, includeLast);
  }

  /**
   * Step up a cell ignoring the oldTemporalEventSource.  See {@link #stepUp(Cell, Duration, boolean)}.
   * @param cell the Cell to step up
   * @param maxTime the time beyond which Events are ignored
   * @param includeMaxTime whether to apply the Events occurring at maxTime
   */
  public void stepUpSimple(final Cell<?> cell, final Duration maxTime, final boolean includeMaxTime) {
    final NavigableMap<Duration, EventGraph<Event>> subTimeline;
    var cellTimePair = getCellTime(cell);
    var cellTime = cellTimePair.getLeft();
    var cellSteppedAtTime = cellTimePair.getRight();
    if (cellTime.longerThan(maxTime)) {
      throw new UnsupportedOperationException("Trying to step cell from the past");
    }
    try {
      final TreeMap<Duration, EventGraph<Event>> eventsByTimeForTopic = eventsByTopic.get(cell.getTopic());
      if (eventsByTimeForTopic == null) {
        if (maxTime.longerThan(cellTime) && maxTime.shorterThan(Duration.MAX_VALUE)) {
          cell.step(maxTime.minus(cellTime));
          putCellTime(cell, maxTime, false);
        }
        return;
      }
      subTimeline = eventsByTimeForTopic.subMap(cellTime, true, maxTime, includeMaxTime);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    for (Map.Entry<Duration, EventGraph<Event>> e : subTimeline.entrySet()) {
      final EventGraph<Event> eventGraph = e.getValue();
      var delta = e.getKey().minus(cellTime);
      if (delta.isPositive()) {
        cell.step(delta);
      } else if (delta.isNegative()) {
        throw new UnsupportedOperationException("Trying to step cell from the past");
      }
      cellTimePair = getCellTime(cell);
      if (cellTimePair.getLeft().isEqualTo(e.getKey()) && cellTimePair.getRight()) {
        // We've already applied this graph; not doing it twice!
      } else {
        cell.apply(eventGraph, null, false);
        putCellTime(cell, e.getKey(), true);
      }
    }
  }

  /**
   * Step up the Cell through the timeline of EventGraphs.  Stepping up means to
   * apply Effects from Events up to some point in time.
   *
   * @param cell the Cell to step up
   * @param maxTime the time beyond which Events are ignored
   * @param includeMaxTime whether to apply the Events occurring at maxTime
   */
  public void stepUp(final Cell<?> cell, final Duration maxTime, final boolean includeMaxTime) {
    // Separate out the simpler case of no past simulation for readability
    if (oldTemporalEventSource == null) {
      stepUpSimple(cell, maxTime, includeMaxTime);
      return;
    }

    // Get the relevant submap of EventGraphs for both the old and new timelines.
    final NavigableMap<Duration, EventGraph<Event>> subTimeline;
    final NavigableMap<Duration, EventGraph<Event>> oldSubTimeline;
    var cellTimePair = getCellTime(cell);
    var cellTime = cellTimePair.getLeft();
    final var originalCellTime = cellTime;
    var cellSteppedAtTime = cellTimePair.getRight();
    final var originalCellSteppedAtTime = cellSteppedAtTime;
    if (cellTime.longerThan(maxTime)) {
      throw new UnsupportedOperationException("Trying to step cell from the past");
    }
    try {
      var t = cell.getTopic();
      var m = eventsByTopic.get(t);
      subTimeline = m == null ? null : m.subMap(cellTime, true, maxTime, includeMaxTime);
      var mo = oldTemporalEventSource.eventsByTopic.get(t);
      oldSubTimeline = mo == null ? null : mo.subMap(cellTime, true, maxTime, includeMaxTime);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Initialize submap entries and iterators
    var iter = subTimeline == null ? null : subTimeline.entrySet().iterator();
    var entry = iter != null && iter.hasNext() ? iter.next() : null;
    var entryTime = entry == null ? Duration.MAX_VALUE : entry.getKey();
    var oldCell = getOldCell(cell).orElseThrow();
    var oldCellTimePair = oldTemporalEventSource.getCellTime(cell);
    var oldCellTime = oldCellTimePair.getLeft();
    final var originalOldCellTime = oldCellTime;
    var oldCellSteppedAtTime = oldCellTimePair.getRight();
    final var originalOldCellStoppedAtTime = oldCellSteppedAtTime;
    var oldIter = oldSubTimeline == null ? null : oldSubTimeline.entrySet().iterator();
    var oldEntry = oldIter != null && oldIter.hasNext() ? oldIter.next() : null;
    var oldEntryTime = oldEntry == null ? Duration.MAX_VALUE : oldEntry.getKey();
    var stale = TemporalEventSource.this.isTopicStale(cell.getTopic(), cellTime);

    // Each iteration of this loop processes a time with an EventGraph; else just steps up to maxTime.
    // The cell applies both the old and new EventGraphs except only the new when at the same timepoint.
    // An old cell is created and/or stepped just within the old TemporalEventSource to determine if the
    // new cell becomes stale or unstale.  The old cell is abandoned when not stale and when there are no
    // new EventGraphs, which are just changes (additions and replacements) on top of the old.
    int done = 0;
    while (done < 2) {
      boolean stepped = false;

      // step(timeDelta) for oldCell if necessary
      if (stale) {  // Only step if the topic is stale
        var minWrtOld = Duration.min(entryTime, oldEntryTime, maxTime);
        if (oldCellTime.shorterThan(minWrtOld) && minWrtOld.shorterThan(Duration.MAX_VALUE)) {
          stepped = true;
          oldCell.step(minWrtOld.minus(oldCellTime));
          oldCellTime = minWrtOld;
          oldCellSteppedAtTime = false;
          oldTemporalEventSource.putCellTime(oldCell, oldCellTime, oldCellSteppedAtTime);
        }
      }
      // step(timeDelta) for oldCell if necessary
      var minWrtNew = Duration.min(entryTime, oldEntryTime, maxTime);
      if (cellTime.shorterThan(minWrtNew) && minWrtNew.shorterThan(Duration.MAX_VALUE)) {
        stepped = true;
        cell.step(minWrtNew.minus(cellTime));
        cellTime = minWrtNew;
        cellSteppedAtTime = false;
      }

      // check staleness
      boolean timesAreEqual = stale && cellTime.isEqualTo(oldCellTime); // inserted stale thinking it would be faster to skip isEqualTo()
      if (stale && stepped && timesAreEqual) {
        stale = updateStale(cell, oldCell);
      }

      // Apply old EventGraph
      boolean oldCellStateChanged = false;
      boolean cellStateChanged = false;
      if (oldEntry != null &&
          oldEntryTime.isEqualTo(cellTime) &&
          (oldCellTime.shorterThan(maxTime) || (includeMaxTime && oldCellTime.isEqualTo(maxTime)))) {
        var unequalGraphs = entry != null && entryTime.isEqualTo(oldEntryTime) && !oldEntry.getValue().equals(entry.getValue());

        // Step old cell if stale or if the new EventGraph is changed
        final var eventGraph = oldEntry.getValue();
        if (stale || unequalGraphs) {
          // If topic is not stale, and old cell is not stepped up, then it was abandoned, and need to create a new one.
          if (!stale && unequalGraphs && !oldCellTime.isEqualTo(cellTime)) {
            //cellCache.computeIfAbsent(cell.getTopic(), $ -> new TreeMap<>()).put(oldCellTime, oldCell);
            oldCell = cell.duplicate();  // Would stepping up old cell be faster in some cases?
            oldCellTime = cellTime;
            oldCellStateChanged = true;
          }
          final var oldOldState = oldCell.getState(); // getState() generates a copy, so oldState won't change
          if (!originalOldCellTime.isEqualTo(oldCellTime) || !originalOldCellStoppedAtTime) {
            oldCell.apply(eventGraph, null, false);
            oldCellSteppedAtTime = true;
          }
          oldTemporalEventSource.putCellTime(oldCell, oldCellTime, oldCellSteppedAtTime);
          oldCellStateChanged = oldCellStateChanged || !oldCell.getState().equals(oldOldState);
        }

        // Step up new cell if no new EventGraph at this time.
        if (entry == null || entryTime.longerThan(oldEntryTime) || unequalGraphs) {
          final var oldState = cell.getState(); // getState() generates a copy, so oldState won't change
          if (!originalCellTime.isEqualTo(cellTime) || !originalCellSteppedAtTime) {
            cell.apply(eventGraph, null, false);
            cellSteppedAtTime = true;
          }
          cellStateChanged = !cell.getState().equals(oldState);
        }
        oldEntry = oldIter != null && oldIter.hasNext() ? oldIter.next() : null;
        oldEntryTime = oldEntry == null ? Duration.MAX_VALUE : oldEntry.getKey();
      }

      // Apply new EventGraph
      if (entry != null && entryTime.isEqualTo(cellTime) &&
          (cellTime.shorterThan(maxTime) || (includeMaxTime && cellTime.isEqualTo(maxTime)))) {
        final var eventGraph = entry.getValue();
        final var oldState = cell.getState(); // getState() generates a copy, so oldState won't change
        if (!originalCellTime.isEqualTo(cellTime) || !originalCellSteppedAtTime) {
          cell.apply(eventGraph, null, false);
          cellSteppedAtTime = true;
        }
        cellStateChanged = !cell.getState().equals(oldState);
        entry = iter != null && iter.hasNext() ? iter.next() : null;
        entryTime = entry == null ? Duration.MAX_VALUE : entry.getKey();
      }

      // check staleness
      if (timesAreEqual && (stale || cellStateChanged || oldCellStateChanged)) {
        stale = updateStale(cell, oldCell);
      }
      if ( !( (cellTime.shorterThan(maxTime) || (stale && oldCellTime.shorterThan(maxTime))) &&
              (entry != null || oldEntry != null) ) ) {
        ++done;
      }

    }
    putCellTime(cell, cellTime, cellSteppedAtTime);
  }

  protected boolean updateStale(Cell<?> cell, Cell<?> oldCell) {
    var cellTimePair = getCellTime(cell);
    var time = cellTimePair.getLeft();
    // var steppedAtTime = cellTimePair.getRight();  // TODO: Should staleness be specified as before/after events at time like cellTimes?
    boolean stale = !cell.getState().equals(oldCell.getState());
    boolean wasStale = isTopicStale(cell.getTopic(), time);
    if (stale && !wasStale) {
      setTopicStale(cell.getTopic(), time);
    } else if (!stale && wasStale) {
      setTopicUnstale(cell.getTopic(), time);
    }
    return stale;
  }

  public <State> Cell<State> getCell(Topic<?> topic, Duration maxTime, boolean includeMaxTime) {
    Optional<LiveCell<?>> cell = liveCells.getCells(topic).stream().findFirst();
    if (cell.isEmpty()) {
      throw new RuntimeException("Can't find cell for query.");
    }
    return getCell((Cell<State>)cell.get().get(), maxTime, includeMaxTime);
  }

  public <State> Cell<State> getCell(Cell<State> cell, Duration maxTime, boolean includeMaxTime) {
    var time = getCellTime(cell).getLeft();
    // Use the one in LiveCells if not asking for a time in the past.
    if (time == null || time.noLongerThan(maxTime)) {
      stepUp(cell, maxTime, includeMaxTime);
      return cell;
    }
    // For a cell in the past, use the cell cache
    Cell<State> pastCell = getOrCreateCellInCache(cell.getTopic(), maxTime, includeMaxTime);
    return pastCell;
  }

  public <State> Cell<State> getCell(Query<State> query, Duration maxTime, boolean includeMaxTime) {
    Optional<LiveCell<State>> cell = liveCells.getLiveCell(query);
    if (cell.isEmpty()) {
      throw new RuntimeException("Can't find cell for query.");
    }
    return getCell(cell.get().get(), maxTime, includeMaxTime);
  }

  public <State> Cell<State> getOrCreateCellInCache(Topic<?> topic, Duration maxTime, boolean includeMaxTime) {
    final TreeMap<Duration, Cell<?>> inner = cellCache.computeIfAbsent(topic, $ -> new TreeMap<>());
    final Map.Entry<Duration, Cell<?>> entry = inner.floorEntry(maxTime);
    Cell<?> cell;
    if (entry != null) {
      cell = entry.getValue();
      // TODO: maybe pass in boolean for whether to duplicate the cell in the cache instead of removing and adding back after stepping up
      inner.remove(entry.getKey());
    } else {
      cell = missionModel.getInitialCells().getCells(topic).stream().findFirst().orElseThrow().get().duplicate();
    }
    stepUp(cell, maxTime, includeMaxTime);
    inner.put(maxTime, cell);
    return (Cell<State>)cell; // TODO: avoid this force cast and associated compiler warning
  }

  public Optional<LiveCell<?>> getOldCell(LiveCell<?> cell) {
    if (oldTemporalEventSource == null) return Optional.empty();
    return oldTemporalEventSource.liveCells.getCells(cell.get().getTopic()).stream().findFirst();
  }

  public Optional<Cell<?>> getOldCell(Cell<?> cell) {
    if (oldTemporalEventSource == null) return Optional.empty();
    return oldTemporalEventSource.liveCells.getCells(cell.getTopic()).stream().findFirst().map(LiveCell::get);
  }

  public Pair<Duration, Boolean> getCellTime(Cell<?> cell) {
    var cellTime = cellTimes.get(cell);
    if (cellTime == null) {
      return Pair.of(Duration.ZERO, false);
    }
    Boolean cellStepped = this.cellTimeStepped.get(cell);
    if (cellStepped == null) {
      this.cellTimeStepped.put(cell, false);
      cellStepped = false;
    }
    return Pair.of(cellTime, cellStepped);
  }

  public void putCellTime(Cell<?> cell, Duration cellTime, boolean cellStepped) {
    this.cellTimes.put(cell, cellTime);
    this.cellTimeStepped.put(cell, cellStepped);
  }


  @Override
  public TemporalCursor cursor() {
    return new TemporalCursor();
  }

  public final class TemporalCursor implements Cursor {
    private final Iterator<TimePoint> iterator;

    TemporalCursor(Iterator<TimePoint> iterator) {
      this.iterator = iterator;
    }

    private TemporalCursor() {
      this(TemporalEventSource.this.iterator());
    }

    @Override
    public void stepUp(final Cell<?> cell) {
      TemporalEventSource.this.stepUp(cell, Duration.MAX_VALUE, true);
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
