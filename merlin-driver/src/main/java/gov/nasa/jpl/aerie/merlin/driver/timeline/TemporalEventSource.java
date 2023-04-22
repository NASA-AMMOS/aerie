package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.engine.SlabList;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TemporalEventSource implements EventSource, Iterable<TemporalEventSource.TimePoint> {
  public LiveCells liveCells;
  private final MissionModel<?> missionModel;
  public SlabList<TimePoint> points = new SlabList<>();
  public TreeMap<Duration, EventGraph<Event>> eventsByTime = new TreeMap<>();  // TODO: REVIEW - Could do binary search on slab list if
                                                                               //       a list of time-graph pairs instead of deltas.
  public Map<Topic<?>, TreeMap<Duration, EventGraph<Event>>> eventsByTopic = new HashMap<>();  // TODO: REVIEW - Could be slab list like eventsByTime
  public Map<TaskId, TreeMap<Duration, EventGraph<Event>>> eventsByTask = new HashMap<>();
  public Map<EventGraph<Event>, Set<Topic<?>>> topicsForEventGraph = new HashMap<>();
  public Map<EventGraph<Event>, Set<TaskId>> tasksForEventGraph = new HashMap<>();
  public Map<EventGraph<Event>, Duration> timeForEventGraph = new HashMap<>();
  public HashMap<Cell<?>, Duration> cellTimes = new HashMap<>();
  public Optional<TemporalEventSource> oldTemporalEventSource;
  /**
   * cellCache keeps duplicates and old cells that can be reused to more quickly get a past cell value.
   * For example, if a task needs to re-run but starts in the past, we can re-run it from a past point,
   * and successive reads a cell can use a duplicate cached cell stepped up from its initial state.
   */
  private final HashMap<Topic<?>, TreeMap<Duration, LiveCell<?>>> cellCache = new HashMap<>();



  /** When topics/cells become stale */
  public final Map<Topic<?>, TreeMap<Duration, Boolean>> staleTopics = new HashMap<>();


  public TemporalEventSource() {
    this(null, null, Optional.empty());
  }

  public TemporalEventSource(
      final LiveCells liveCells,
      final MissionModel<?> missionModel,
      final Optional<TemporalEventSource> oldTemporalEventSource)
  {
    this.liveCells = liveCells;
    this.missionModel = missionModel;
    this.oldTemporalEventSource = oldTemporalEventSource;
    // Assumes the current time is zero, and the cells have not yet been stepped.
    // FIXME: LiveCells creates duplicates of cells in its parent as they are queried; they will need celltimes, too.
    //        Maybe if cellTimes.get() can be wrapped such that it inserts 0 if absent.
    if (liveCells != null) {
      for (LiveCell liveCell : liveCells.getCells()) {
        final Cell cell = liveCell.get();
        cellTimes.put(cell, Duration.ZERO);
      }
    }
  }

  public TemporalEventSource(LiveCells liveCells) {
    this(liveCells, null, Optional.empty());
  }

  public void add(final Duration delta) {
    if (delta.isZero()) return;
    this.points.append(new TimePoint.Delta(delta));
  }

  public void add(final EventGraph<Event> graph, Duration time) {
    var topics = extractTopics(graph);
    this.points.append(new TimePoint.Commit(graph, topics));
    addIndices(graph, time, topics);
  }

  /**
   * Index the graph by time, topic, and task.
   * @param graph the graph of Events to add
   * @param time the time as a Duration when the events occur
   */
  protected void addIndices(final EventGraph<Event> graph, Duration time, Set<Topic<?>> topics) {
    eventsByTime.put(time, graph);
    if (topics == null) topics = extractTopics(graph);
    var tasks = extractTasks(graph);
    topics.forEach(t -> this.eventsByTopic.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, graph));
    tasks.forEach(t -> this.eventsByTask.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, graph));
    // TODO: REVIEW -- do we really need all these maps?
    topicsForEventGraph.computeIfAbsent(graph, $ -> new TreeSet<>()).addAll(topics);  // Tree over Hash for less memory/space
    tasksForEventGraph.computeIfAbsent(graph, $ -> new TreeSet<>()).addAll(tasks);
  }

  public void replaceEventGraph(EventGraph<Event> oldG, EventGraph<Event> newG) {
    // TODO: Maybe the handling of the new graph could be in a separate put(EventGraph), and that of the old in a remove(EventGraph).
    //       There is an add() that serves as a put().
    //       See addIndices().
    // TODO: Something doesn't feel right here.  See how these maps are handled elsewhere; this seems like too much work.
    //       Maybe we should separate this into three methods (for time, task, and topic), and pass an extra arg for
    //       whether the tasks, for example, changed, so that extractTasks() could be avoided.
    // HERE!!!  BTW, MAKE A TODO LIST TO FINISH INCREMENTAL SIM!

    // time
    Duration time = timeForEventGraph.get(oldG);
    timeForEventGraph.remove(time);
    timeForEventGraph.put(newG, time);
    eventsByTime.put(time, newG);

    // task
    var tasks = tasksForEventGraph.get(oldG);
    tasks.forEach(t -> eventsByTask.get(t).remove(oldG));
    tasksForEventGraph.remove(oldG);
    var newTasks = extractTasks(newG);
    tasksForEventGraph.put(newG, newTasks);
    newTasks.forEach(t -> eventsByTask.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, newG));

    // topic
    var topics = topicsForEventGraph.get(oldG);
    topics.forEach(t -> eventsByTopic.get(t).remove(oldG));
    topicsForEventGraph.remove(oldG);
    var newTopics = extractTopics(newG);
    topicsForEventGraph.put(newG, newTopics);
    newTopics.forEach(t -> eventsByTopic.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, newG));
  }


  @Override
  public Iterator<TimePoint> iterator() {
    if (oldTemporalEventSource.isEmpty()) {
      return TemporalEventSource.this.points.iterator();
    }
    // Create an iterator that combines the old and new EventGraph timelines
    // This TemporalEventSource only keeps modifications of EventGraphs from the oldTemporalEventSource.
    return new Iterator<>() {
      private Iterator<TemporalEventSource.TimePoint> oldIter = oldTemporalEventSource.get().iterator();
      private Duration accumulatedDuration = Duration.ZERO;
      private Duration lastTime = Duration.ZERO;
      private TemporalEventSource.TimePoint peek = null;
      private Iterator<Map.Entry<Duration, EventGraph<Event>>> riter =
          TemporalEventSource.this.eventsByTime.entrySet().iterator();
      private  Map.Entry<Duration, EventGraph<Event>> rpeek = null;

      @Override
      public boolean hasNext() {
        if (peek != null) return true;
        if (rpeek != null) return true;
        if (oldIter.hasNext()) return true;
        if (riter.hasNext()) return true;
        return false;
      }

      @Override
      public TemporalEventSource.TimePoint next() {
        // TODO: This essentially builds a new list of TimePoints like this.points.
        //       If we're going to use this iterator a lot, then should save and reuse it?
        //       May need to check for staleness.

        // Get next peek and rpeek values if null, calling iter.next() and riter.next()
        if (peek == null && oldIter.hasNext()) {
          peek = oldIter.next();
          if (peek instanceof TimePoint.Delta d) {
            accumulatedDuration = d.delta().plus(accumulatedDuration);
          }
        }
        if (rpeek == null && riter.hasNext()) {
          rpeek = riter.next();
        }
        // If we didn't get anything, then we have no elements and throw an exception
        if (peek == null && rpeek == null) {
          if (oldIter.hasNext() || riter.hasNext()) throw new AssertionError();
          throw new NoSuchElementException();
        }

        // Determine if the replacement or original TimePoint is next,
        // construct TimePoint to return if necessary,
        // and update peek, rpeek, accumulatedTime, and lastTime.
        //
        // First check if replacement is next
        if (rpeek != null && (peek == null || rpeek.getKey().noLongerThan(accumulatedDuration))) {
          // We may need to create a TimePoint.Delta before the Commit
          Duration delta = rpeek.getKey().minus(lastTime);
          // If this delta happens to be the same as the Delta in this.points, use the existing Delta
          if (peek != null && peek instanceof TimePoint.Delta tpd && tpd.delta().isEqualTo(delta)) {
            peek = null;  // means we used it and need the next one
            lastTime = rpeek.getKey();
            return tpd;
          }
          // Construct and return a TimePoint.Delta if non-zero
          if (delta.isPositive()) {
            TimePoint tp = new TimePoint.Delta(delta);
            lastTime = rpeek.getKey();
            return tp;
          }
          // Sanity check - delta must be zero here
          if (!delta.isZero()) throw new AssertionError();

          // If this is the same time as the next Commit (or Delta) on this.points, replace and eat the TimePoint
          if (lastTime.isEqualTo(accumulatedDuration)) {
            peek = null;  // means we used it and need the next one
          }

          // Now, finally construct a Commit from the replacement EventGraph
          TimePoint tp = new TimePoint.Commit(rpeek.getValue(), topicsForEventGraph.get(rpeek.getValue()));
          rpeek = null; // means we used it and need the next one
          return tp;
        }
        // Check if the original TimePoint is next
        if (peek != null && (rpeek == null || rpeek.getKey().longerThan(accumulatedDuration))) {
          // If this TimePoint is a Delta, make sure we get the change in time (aka delta) since lastTime
          if (peek instanceof TimePoint.Delta d) {
            final TimePoint tp;
            // Reuse the existing Delta if we can
            if (lastTime.plus(d.delta()).isEqualTo(accumulatedDuration)) {
              tp = d;
            } else {
              tp = new TimePoint.Delta(accumulatedDuration.minus(lastTime));
            }
            lastTime = accumulatedDuration;
            peek = null;  // means we used it and need the next one
            return tp;
          }
          // peek is an unreplaced Commit; return it
          var commit = peek;
          peek = null;  // means we used it and need the next one
          return commit;
        }
        // Shouldn't get here
        throw new AssertionError("Impossible case in TemporalEventSourceDelta.next()");
      }
    };
  }


  public Boolean setTopicStale(Topic<?> topic, Duration offsetTime) {
    return staleTopics.computeIfAbsent(topic, $ -> new TreeMap<>()).put(offsetTime, true);
  }

  public Boolean setTopicUnstale(Topic<?> topic, Duration offsetTime) {
    return staleTopics.computeIfAbsent(topic, $ -> new TreeMap<>()).put(offsetTime, false);
  }

  /**
   * Determine whether a topic been marked stale at a specified time.
   * @param topic topic to check for staleness
   * @param timeOffset the staleness time
   * @return true if the topic is marked stale at timeOffset
   */
  public boolean isTopicStale(Topic<?> topic, Duration timeOffset) {
    var map = this.staleTopics.get(topic);
    final Duration staleTime = map.floorKey(timeOffset);
    return staleTime != null && map.get(staleTime);
  }



  /**
   * Step up the Cell for one set of Events (an EventGraph) up to a specified last Event.  Stepping up means to
   * apply Effects from Events up to some point in time.  The EventGraph represents partially time-ordered events.
   * Thus, the Cell may be stepped up to an Event within that partial order.
   *
   * Staleness is not checked here and must be handled by the caller.
   *
   * @param cell the Cell to step up
   * @param events the Events that may affect the Cell
   * @param lastEvent a boundary within the graph of Events beyond which Events are not applied
   * @param includeLast whether to apply the Effect of the last Event
   */
  public void stepUp(final Cell<?> cell, EventGraph<Event> events, final Optional<Event> lastEvent, final boolean includeLast) {
    cell.apply(events, lastEvent, includeLast);
  }

  /**
   * Step up the Cell through the timeline of EventGraphs.  Stepping up means to
   * apply Effects from Events up to some point in time.
   *
   * @param cell the Cell to step up
   * @param maxTime the time beyond which Events are ignored
   * @param includeMaxTime whether to apply the Events occurring at maxTime
   */
  public void stepUp(final LiveCell<?> cell, final Duration maxTime, final boolean includeMaxTime) {
    cell.cursor.stepUp(cell.get(), maxTime, includeMaxTime);
  }

  public <State> LiveCell<State> getCell(Topic<?> topic, Duration maxTime, boolean includeMaxTime) {
    Optional<LiveCell<?>> cell = liveCells.getCells(topic).stream().findFirst();
    if (cell.isEmpty()) {
      throw new RuntimeException("Can't find cell for query.");
    }
    return getCell((LiveCell<State>)cell.get(), maxTime, includeMaxTime);
  }

  public <State> LiveCell<State> getCell(LiveCell<State> cell, Duration maxTime, boolean includeMaxTime) {
    var time = cellTimes.get(cell.get());
    // Use the one in LiveCells if not asking for a time in the past.
    if (time == null || time.noLongerThan(maxTime)) {
      stepUp(cell, maxTime, includeMaxTime);
      cellTimes.put(cell.get(), maxTime);
      return cell;
    }
    // For a cell in the past, use the cell cache
    LiveCell<State> liveCell = getOrCreateCellInCache(cell.get().getTopic(), maxTime, includeMaxTime);
    return liveCell;
  }

  public <State> LiveCell<State> getCell(Query<State> query, Duration maxTime, boolean includeMaxTime) {
    Optional<LiveCell<State>> cell = liveCells.getLiveCell(query);
    return getCell(cell.get(), maxTime, includeMaxTime);
  }

  public <State> LiveCell<State> getOrCreateCellInCache(Topic<?> topic, Duration maxTime, boolean includeMaxTime) {
    final TreeMap<Duration, LiveCell<?>> inner = cellCache.computeIfAbsent(topic, $ -> new TreeMap<>());
    final Map.Entry<Duration, LiveCell<?>> entry = inner.floorEntry(maxTime);
    LiveCell<?> cell;
    if (entry != null) {
      cell = entry.getValue();
      // TODO: maybe pass in boolean for whether to duplicate the cell in the cache instead of removing and adding back after stepping up
      inner.remove(entry.getKey());
    } else {
      cell = missionModel.getInitialCells().getCells(topic).stream().findFirst().get();
      cell = new LiveCell<>(cell.get().duplicate(), cursor());
    }
    stepUp(cell, maxTime, includeMaxTime);
    inner.put(maxTime, cell);
    return (LiveCell<State>) cell;
  }

  public LiveCell<?> getOldCell(LiveCell<?> cell) {
    if (oldTemporalEventSource.isEmpty()) return null;
    return oldTemporalEventSource.get().liveCells.getCells(cell.get().getTopic()).stream().findFirst().get();
  }

  public Cell<?> getOldCell(Cell<?> cell) {
    if (oldTemporalEventSource.isEmpty()) return null;
    return oldTemporalEventSource.get().liveCells.getCells(cell.getTopic()).stream().findFirst().get().get();
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

    public void stepUp(final Cell<?> cell, EventGraph<Event> events, final Optional<Event> lastEvent, final boolean includeLast) {
      TemporalEventSource.this.stepUp(cell, events, lastEvent, includeLast);
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
      // TODO: If the cell is not stale, can't we avoid stepping both cells until there's a change in EventGraphs,
      //       at which point we can duplicate the stepped cell or copy the cell's state?
      // TODO: And, don't we want to stop stepping up if are no more changes to Events?  Or, is that handled at a
      //       higher level, and we just need to step all the way to maxTime?
      // TODO: Should we take into account the plan horizon here or assume that's done at a higher level?
      // TODO: The above may be answered by looking where step() and stepUp() are called, like in LiveCell.get()
      final NavigableMap<Duration, EventGraph<Event>> subTimeline;
      NavigableMap<Duration, EventGraph<Event>> oldSubTimeline = null;
      var cellTime = cellTimes.get(cell);
      if (cellTime.longerThan(maxTime)) {
        throw new UnsupportedOperationException("Trying to step cell from the past");
      }
      try {
         subTimeline =
            eventsByTopic.get(cell.getTopic()).subMap(cellTime, true, maxTime, includeMaxTime);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      if (oldTemporalEventSource.isEmpty()) {
        for (Map.Entry<Duration, EventGraph<Event>> e : subTimeline.entrySet()) {
          final EventGraph<Event> p = e.getValue();
          var delta = e.getKey().minus(cellTime);
          if (delta.isPositive()) {
            cell.step(delta);
          } else if (delta.isNegative()) {
            throw new UnsupportedOperationException("Trying to step cell from the past");
          }
          cell.apply(p, null, false);
          cellTimes.put(cell, e.getKey());
        }
        return;
      }
      try {
        oldSubTimeline =
               oldTemporalEventSource.get().eventsByTopic.get(cell.getTopic()).subMap(cellTime, true,
                                                                                      maxTime, includeMaxTime);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      var iter = subTimeline.entrySet().iterator();
      var entry = iter.hasNext() ? iter.next() : null;
      var entryTime = entry == null ? Duration.MAX_VALUE : entry.getKey();
      var oldCell = getOldCell(cell);
      var oldCellTime = oldTemporalEventSource.get().cellTimes.get(oldCell);
      var oldIter = oldSubTimeline.entrySet().iterator();
      var oldEntry = oldIter.hasNext() ? oldIter.next() : null;
      var oldEntryTime = oldEntry == null ? Duration.MAX_VALUE : oldEntry.getKey();
      var stale = TemporalEventSource.this.isTopicStale(cell.getTopic(), cellTime);
      while ((entry != null || oldEntry != null) &&
             (cellTime.shorterThan(maxTime) || oldCellTime.shorterThan(maxTime))) {
        boolean timesWereEqual = cellTime.isEqualTo(oldCellTime);
        boolean stepped = false;
        // check if need to step(delta) for oldCell
        var minWrtOld = Duration.min(timesWereEqual ? Duration.MAX_VALUE : cellTime, entryTime, oldEntryTime, maxTime);
        if (oldCellTime.shorterThan(minWrtOld)) {
          stepped = true;
          oldCell.step(oldCellTime.minus(minWrtOld));
          oldCellTime = minWrtOld;
          oldTemporalEventSource.get().cellTimes.put(oldCell, oldCellTime);
        }
        // check if need to step(delta) for cell
        var minWrtNew = Duration.min(timesWereEqual ? Duration.MAX_VALUE : oldCellTime, entryTime, oldEntryTime, maxTime);
        if (cellTime.shorterThan(minWrtOld)) {
          stepped = true;
          cell.step(cellTime.minus(minWrtNew));
          cellTime = minWrtNew;
          cellTimes.put(cell, cellTime);
        }
        // check staleness
        boolean timesAreEqual = cellTime.isEqualTo(oldCellTime);
        if (stepped && timesAreEqual) {
          stale = updateStale(cell, oldCell);
        }
        // check if need to apply EventGraphs
        boolean cellStateChanged = false;
        if (entry != null && entryTime.isEqualTo(cellTime) &&
            (cellTime.shorterThan(maxTime) || (includeMaxTime && cellTime.isEqualTo(maxTime)))) {
          final var eventGraph = entry.getValue();
          final var oldState = cell.getState(); // getState() generates a copy, so oldState won't change
          cell.apply(eventGraph, null, false);
          cellStateChanged = !cell.getState().equals(oldState);
          entry = iter.hasNext() ? iter.next() : null;
          entryTime = entry == null ? Duration.MAX_VALUE : entry.getKey();
        }
        boolean oldCellStateChanged = false;
        if (oldEntry != null && oldEntryTime.isEqualTo(oldCellTime) &&
            (oldCellTime.shorterThan(maxTime) || (includeMaxTime && oldCellTime.isEqualTo(maxTime)))) {
          final var eventGraph = oldEntry.getValue();
          final var oldOldState = oldCell.getState(); // getState() generates a copy, so oldState won't change
          oldCell.apply(eventGraph, null, false);
          oldCellStateChanged = !oldCell.getState().equals(oldOldState);
          oldEntry = oldIter.hasNext() ? oldIter.next() : null;
          oldEntryTime = oldEntry == null ? Duration.MAX_VALUE : oldEntry.getKey();
        }
        // check staleness
        if (timesAreEqual && (cellStateChanged || oldCellStateChanged)) {
          stale = updateStale(cell, oldCell);
        }
        // check if need to step to maxTime
        if (entry == null && oldEntry == null && maxTime.shorterThan(Duration.MAX_VALUE) && stale) {
          if (cellTime.shorterThan(maxTime)) {
            cell.step(cellTime.minus(maxTime));
            cellTime = maxTime;
            cellTimes.put(cell, maxTime);
          }
          if (oldCellTime.shorterThan(maxTime)) {
            oldCell.step(oldCellTime.minus(maxTime));
            oldCellTime = maxTime;
            oldTemporalEventSource.get().cellTimes.put(oldCell, maxTime);
          }
        }
      }
    }

    protected boolean updateStale(Cell<?> cell, Cell<?> oldCell) {
      var time = cellTimes.get(cell);
      boolean stale = !cell.getState().equals(oldCell.getState());
      boolean wasStale = isTopicStale(cell.getTopic(), time);
      if (stale && !wasStale) {
        setTopicStale(cell.getTopic(), time);
      } else if (!stale && wasStale) {
        setTopicUnstale(cell.getTopic(), time);
      }
      return stale;
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
