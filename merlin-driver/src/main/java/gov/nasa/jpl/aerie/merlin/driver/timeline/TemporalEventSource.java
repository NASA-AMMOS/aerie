package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.engine.ResourceId;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TemporalEventSource implements EventSource, Iterable<TemporalEventSource.TimePoint> {
  private static boolean debug = false;
  public LiveCells liveCells;
  private MissionModel<?> missionModel;
  //public SlabList<TimePoint> points = new SlabList<>();  // This is not used for stepping Cells anymore.  Remove?
  public TreeMap<Duration, List<TimePoint.Commit>> commitsByTime = new TreeMap<>();
  public Map<Topic<?>, TreeMap<Duration, List<EventGraph<Event>>>> eventsByTopic = new HashMap<>();
  public Map<TaskId, TreeMap<Duration, List<EventGraph<Event>>>> eventsByTask = new HashMap<>();
  public Map<EventGraph<Event>, Set<Topic<?>>> topicsForEventGraph = new HashMap<>();
  public Map<EventGraph<Event>, Set<TaskId>> tasksForEventGraph = new HashMap<>();
  public Map<EventGraph<Event>, Duration> timeForEventGraph = new HashMap<>();
  HashMap<Cell<?>, Duration> cellTimes = new HashMap<>();
  HashMap<Cell<?>, Integer> cellTimeStepped = new HashMap<>();

  public HashMap<Duration, Set<Topic<?>>> topicsOfRemovedEvents = new HashMap<>();
  /** Times when a resource profile segment should be removed from the simulation results. */
  public HashMap<Duration, Set<String>> removedResourceSegments = new HashMap<>();
  public TemporalEventSource oldTemporalEventSource;
  protected Duration curTime = Duration.MIN_VALUE;

  public Duration curTime() {
    return curTime;
  }

  public void setCurTime(Duration time) {
    curTime = time;
  }

  private static int ctr = 0;
  private final int i = ctr++;

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
        putCellTime(cell, Duration.ZERO, 0);
      }
    }
  }

  public TemporalEventSource(LiveCells liveCells) {
    this(liveCells, null, null);
  }

//  public void add(final Duration delta) {
//    if (delta.isZero()) return;
//    this.points.append(new TimePoint.Delta(delta));
//  }

  public void add(final EventGraph<Event> graph, Duration time) {
    var topics = extractTopics(graph);
    var commit = new TimePoint.Commit(graph, topics);
//    this.points.append(commit);
    addIndices(commit, time, topics);
  }

  /**
   * Index the commit and graph by time, topic, and task.
   * For multiple commits at the same time, we assume addIndices() is called for each commit in the sequential order
   * that they are to be applied.
   * @param commit the commit of Events to add
   * @param time the time as a Duration when the events occur
   */
  protected void addIndices(final TimePoint.Commit commit, Duration time, Set<Topic<?>> topics) {
    commitsByTime.computeIfAbsent(time, $ -> new ArrayList<>()).add(commit);
    final var finalTopics = topics == null ? extractTopics(commit.events) : topics;
    final var tasks = extractTasks(commit.events);
    timeForEventGraph.put(commit.events, time);
    var eventList = commitsByTime.get(time).stream().map(c -> c.events).toList();
    topics.forEach(t -> this.eventsByTopic.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, eventList));
    tasks.forEach(t -> this.eventsByTask.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, eventList));
    // TODO: REVIEW -- do we really need all these maps?
    topicsForEventGraph.computeIfAbsent(commit.events, $ -> HashSet.newHashSet(finalTopics.size())).addAll(topics);
    tasksForEventGraph.computeIfAbsent(commit.events, $ -> HashSet.newHashSet(tasks.size())).addAll(tasks);
  }

  public Map<? extends Topic<?>, TreeMap<Duration, List<EventGraph<Event>>>> getCombinedEventsByTopic() {
    if (oldTemporalEventSource == null) return eventsByTopic;
    if (_eventsByTopic != null && eventsByTopic.size() == _numEventsByTopic) {
      return _eventsByTopic;
    }
    _numEventsByTopic = eventsByTopic.size();
    if (_oldEventsByTopic == null) {
      _oldEventsByTopic = oldTemporalEventSource.getCombinedEventsByTopic();
      oldTemporalEventSource._oldEventsByTopic = null;
    }
    _eventsByTopic = Stream.of(eventsByTopic, _oldEventsByTopic).flatMap(m -> m.entrySet().stream())
        .collect(Collectors.toMap(t -> t.getKey(), t -> t.getValue(), (m1, m2) -> mergeMapsFirstWins(m1, m2)));
    return _eventsByTopic;
  }
  private Map<? extends Topic<?>, TreeMap<Duration, List<EventGraph<Event>>>> _oldEventsByTopic = null;
  private Map<? extends Topic<?>, TreeMap<Duration, List<EventGraph<Event>>>> _eventsByTopic = null;
  private long _numEventsByTopic = 0;

  public static <K, V> TreeMap<K, V> mergeMapsFirstWins(TreeMap<K, V> m1, TreeMap<K, V> m2) {
    if (m1 == null) return m2;
    if (m2 == null || m2.isEmpty()) return m1;
    if (m1.isEmpty()) return m2;
    return Stream.of(m1, m2).flatMap(m -> m.entrySet().stream()).collect(Collectors.toMap(t -> t.getKey(),
                                                                                          t -> t.getValue(),
                                                                                          (v1, v2) -> v1,
                                                                                          TreeMap::new));
  }

  private Duration getTimeForEventGraph(EventGraph<Event> g) {
    var time = timeForEventGraph.get(g);
    if (time == null && oldTemporalEventSource != null) {
      time = oldTemporalEventSource.getTimeForEventGraph(g);
    }
    return time;
  }

  private Set<TaskId> getTasksForEventGraph(EventGraph<Event> g) {
    var tasks = tasksForEventGraph.get(g);
    if (tasks == null && oldTemporalEventSource != null) {
      tasks = oldTemporalEventSource.getTasksForEventGraph(g);
    }
    return tasks;
  }

  private Set<Topic<?>> getTopicsForEventGraph(EventGraph<Event> g) {
    var topics = topicsForEventGraph.get(g);
    if (topics == null && oldTemporalEventSource != null) {
      topics = oldTemporalEventSource.getTopicsForEventGraph(g);
    }
    return topics;
  }

  /**
   * Replace an {@link EventGraph} with another in the various lookup data structures.  {@link EventGraph}s are
   * unique per instance; i.e., {@code equals()} is {@code ==}.  Thus, a graph only occurs at one point in time.
   * This simplifies the implementation.  If the graph to be replaced only exists in the old timeline,
   * {@link TemporalEventSource#oldTemporalEventSource}, then the new graph must be inserted in {@code this}
   * {@link TemporalEventSource} along with any other graphs at the same time in the old timeline.
   *
   * @param oldG the {@link EventGraph} to be replaced
   * @param newG the {@link EventGraph} replacing {@code oldG}
   */
  public void replaceEventGraph(EventGraph<Event> oldG, EventGraph<Event> newG) {
    // Need to replace in this.{timeForEventGraph, commitsByTime, tasksForEventGraph, eventsByTask, topicsForEventGraph,
    // eventsByTopic, points}
    // TODO: points can't be updated, so we should try to remove this.points
    final var newTopics = extractTopics(newG);

    // time - timeForEventGraph
    Duration timeNew = timeForEventGraph.remove(oldG);
    Duration timeOld = oldTemporalEventSource.getTimeForEventGraph(oldG);
    Duration time = timeNew == null ? timeOld : timeNew;
    if (time == null) {
      throw new RuntimeException("Can't find EventGraph to replace!");
    }
    timeForEventGraph.put(newG, time);
    // time - commitsByTime
    var newCommit = new TimePoint.Commit(newG, newTopics);
    var commitList = commitsByTime.get(time);
    if (commitList == null) {
      // copy from old timeline
      commitList = oldTemporalEventSource.getCombinedCommitsByTime().get(time);
      if (commitList != null) {
        commitList = new ArrayList<>(commitList);
      }
    }
    commitList.replaceAll(c -> c.events.equals(oldG) ? newCommit : c);
    commitsByTime.put(time, commitList);

    var eventList = commitsByTime.get(time).stream().map(c -> c.events).toList();

    // task - tasksForEventGraph
    var oldTasks = tasksForEventGraph.remove(oldG);
    final var newTasks = extractTasks(newG);
    tasksForEventGraph.put(newG, newTasks);
    // task - eventsByTask

    // eventsByTask is a Map<TaskId, TreeMap<Duration, List<EventGraph<Event>>>>
    // The list of EventGraphs per Duration includes the list of all EventGraphs in commitsByTime (eventList)
    // whether or not each have the task.
    //
    // There could be a task t in oldG in the old timeline that is not in newG.  this.eventsByTask.get(t).get(time)
    // should be empty if no other EventGraphs at this time include task t, but it's not a problem if the graphs remain,
    // as long as the graphs were replaced.
    if (oldTasks == null) {
      oldTasks = oldTemporalEventSource.getTasksForEventGraph(oldG);
    }
    var allTasks = new HashSet<TaskId>();
    if (oldTasks != null) allTasks.addAll(oldTasks);
    allTasks.addAll(newTasks);
//    final var finalOldTasks = oldTasks;
    allTasks.forEach(t -> {
//      if (finalOldTasks != null && finalOldTasks.contains(t) && !newTasks.contains(t)) {
//        var map = eventsByTask.get(t);
//        if (map != null) {
//          var oldList = map.get(time);
//          if (oldList != null && !oldList.isEmpty()) {
//            map.put(time, eventList);
//          }
//        }
//      } else {
        eventsByTask.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, eventList);
//      }
    });

    // topic - topicsForEventGraph
    var oldTopics = topicsForEventGraph.remove(oldG);
    if (oldTopics == null) {
      oldTopics = oldTemporalEventSource.getTopicsForEventGraph(oldG);
    }
//    final var finalOldTopics = oldTopics;
    topicsForEventGraph.put(newG, newTopics);
    var allTopics = new HashSet<Topic<?>>();
    if (oldTopics != null) allTopics.addAll(oldTopics);
    Set<Topic<?>> lostTopics = oldTopics.stream().filter(t -> !newTopics.contains(t)).collect(Collectors.toSet());
    this.topicsOfRemovedEvents.computeIfAbsent(time, $ -> new HashSet<>()).addAll(lostTopics);
    allTopics.addAll(newTopics);
    allTopics.forEach(t -> {
//      if (finalOldTopics != null && finalOldTopics.contains(t) && !newTopics.contains(t)) {
//        var map = eventsByTopic.get(t);
//        if (map != null) {
//          var oldList = map.get(time);
//          if (oldList != null && !oldList.isEmpty()) {
//            map.put(time, eventList);
//          }
//        }
//      } else {
        eventsByTopic.computeIfAbsent(t, $ -> new TreeMap<>()).put(time, eventList);
//      }
    });
  }

  /**
   * An Iterator for a TreeMap that allows it to grow by appending new entries (i.e. put(k, v) where k is greater than
   * all keys in keySet()).
   *
   * @param <K>
   * @param <V>
   */
  private class TreeMapIterator<K, V> implements Iterator<Map.Entry<K, V>> {

    private TreeMap<K, V> treeMap;
    /** The key of the last entry returned by next() */
    private K lastKey = null;
    private Iterator<Map.Entry<K, V>> iterator = null;
    /** The size of the map when we last checked. If this has changed, then the iterator must be reset based on lastKey */
    private long size;

    private static int ctr = 0;
    private final int i = ctr++;


    public TreeMapIterator(TreeMap<K, V> treeMap) {
      this.treeMap = treeMap;
      size = treeMap.size();
      iterator = treeMap.entrySet().iterator();
      if (debug) System.out.println("" + i + " TreeMapIterator(): " + treeMap);
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
      if (size != treeMap.size()) {  // treeMap has grown, reset iterator
        if (debug) System.out.println("" + i + " TreeMapIterator.hasNext(): size " + size + " <- " + treeMap.size());
        if (debug) System.out.println("" + i + " TreeMapIterator.hasNext(): treeMap = " + treeMap);
        size = treeMap.size();
        if (lastKey == null) {
          iterator = treeMap.entrySet().iterator();
          if (debug) System.out.println("" + i + " TreeMapIterator.hasNext(): iterator <- " + treeMap);
        } else {
          var submap = treeMap.tailMap(lastKey, false);
          if (debug) System.out.println("" + i + " TreeMapIterator.hasNext(): tailMap(lastKey=" + lastKey + ") = " + submap);
          if (submap != null) {
            iterator = submap.entrySet().iterator();
            if (debug) System.out.println("" + i + " TreeMapIterator.hasNext(): iterator <- " + submap);
          } else {
            throw new RuntimeException("no submap!");
          }
        }
      }
      if (iterator != null && iterator.hasNext()) return true;
      return false;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public Map.Entry<K, V> next() {
      if (!hasNext()) throw new NoSuchElementException();
      if (iterator == null) throw new NoSuchElementException();
      var e = iterator.next();
      if (debug) System.out.println("" + i + " TreeMapIterator.next(): lastKey changed from " + lastKey + " to " + e.getKey());
      lastKey = e.getKey();
      if (debug) System.out.println("" + i + " TreeMapIterator.next(): returning " + e);
      return e;
    }
  }

  public class CombinedTreeMapIterator<V, K extends Comparable<K>> implements Iterator<Map.Entry<K, V>> {

    Iterator<Map.Entry<K, V>> i1, i2;
    BiFunction<Entry<K, V>, Entry<K, V>, Entry<K, V>> combiner;
    Map.Entry<K, V> last1 = null;
    Map.Entry<K, V> last2 = null;

    public CombinedTreeMapIterator(final Iterator<Entry<K, V>> i1, final Iterator<Entry<K, V>> i2,
                                   BiFunction<Entry<K, V>, Entry<K, V>, Entry<K, V>> combiner) {
      this.i1 = i1;
      this.i2 = i2;
      this.combiner = combiner;
    }

    @Override
    public boolean hasNext() {
      return last1 != null || last2 != null || i1.hasNext() || i2.hasNext();
    }

    @Override
    public Entry<K, V> next() {
      if (last1 == null && i1.hasNext()) {
        last1 = i1.next();
      }
      if (last2 == null && i2.hasNext()) {
        last2 = i2.next();
      }
      if (last1 == null && last2 == null) {
        throw new NoSuchElementException();
      }
      if (last1 == null) {
        var tmp = last2;
        last2 = null;
        return tmp;
      }
      if (last2 == null) {
        var tmp = last1;
        last1 = null;
        return tmp;
      }
      int c = last1.getKey().compareTo(last2.getKey());
      if (c < 0) {
        var tmp = last1;
        last1 = null;
        return tmp;
      }
      if (c > 0) {
        var tmp = last2;
        last2 = null;
        return tmp;
      }
      var result = combiner.apply(last1, last2);
      last1 = null;
      last2 = null;
      return result;
    }
  }

  /**
   * @return a {@link TreeMap} of {@link TimePoint.Commit}s by time ({@link Duration}) combining
   * the {@link TemporalEventSource#commitsByTime} those of the {@link TemporalEventSource#oldTemporalEventSource}
   * and nested {@link TemporalEventSource#oldTemporalEventSource}s.
   * <p>
   *   The caller should be careful not to modify the returned TreeMap since it might be an actual
   *   {@link TemporalEventSource#commitsByTime}.
   * </p>
   */
  public TreeMap<Duration, List<TimePoint.Commit>> getCombinedCommitsByTime() {
    final var mNew = commitsByTime;
    if (oldTemporalEventSource == null) return mNew;
    final TreeMap<Duration, List<TimePoint.Commit>> mOld;
    if (_combinedCommitsByTime != null && mNew.size() == _numberCommitsByTime) {
      return _combinedCommitsByTime;
    }
    _numberCommitsByTime = mNew.size();
    if (_oldCombinedCommitsByTime != null) {
      mOld = _oldCombinedCommitsByTime;
    } else {
      mOld = oldTemporalEventSource.getCombinedCommitsByTime();
      _oldCombinedCommitsByTime = mOld;
      oldTemporalEventSource._oldCombinedCommitsByTime = null;
    }
    _combinedCommitsByTime = mergeMapsFirstWins(mNew, mOld);
    return _combinedCommitsByTime;
  }
  private TreeMap<Duration, List<TimePoint.Commit>> _oldCombinedCommitsByTime = null;
  private TreeMap<Duration, List<TimePoint.Commit>> _combinedCommitsByTime = null;
  private long _numberCommitsByTime = 0;


  private class TimePointIteratorFromCommitMap implements Iterator<TimePoint> {

    private Iterator<Map.Entry<Duration, List<TimePoint.Commit>>> i;
    private Duration time = Duration.ZERO;
    private Map.Entry<Duration, List<TimePoint.Commit>> lastEntry = null;
    private Iterator<TimePoint.Commit> commitIter = null;

    public TimePointIteratorFromCommitMap(Iterator<Map.Entry<Duration, List<TimePoint.Commit>>> i) {
      this.i = i;
    }

    @Override
    public boolean hasNext() {
      if (commitIter != null && commitIter.hasNext()) return true;
      if (i.hasNext()) return true;
      if (lastEntry != null) {
        if (lastEntry.getKey().longerThan(time)) return true;
        if (!lastEntry.getValue().isEmpty()) return true;
      }
      return false;
    }

    public TimePoint peek() {  // why do I always want this??!!
      return null;
    }

    @Override
    public TimePoint next() {
      if (commitIter != null) {
        if (commitIter.hasNext()) {
          return commitIter.next();
        } else {
          commitIter = null;
        }
      }
      if (lastEntry == null) lastEntry = i.next();
      if (lastEntry.getKey().longerThan(time)) {
        var delta = new TimePoint.Delta(lastEntry.getKey().minus(time));
        time = lastEntry.getKey();
        commitIter = lastEntry.getValue().iterator();
        lastEntry = null;
        return delta;
      }
      commitIter = lastEntry.getValue().iterator();
      while (!commitIter.hasNext()) {
        if (!i.hasNext()) {
          throw new NoSuchElementException();
        }
        lastEntry = i.next();
        commitIter = lastEntry.getValue().iterator();
      }
      if (commitIter.hasNext()) {
        lastEntry = null;
        return commitIter.next();
      }
      throw new NoSuchElementException();
    }
  }

  @Override
  public Iterator<TimePoint> iterator() {
    // Create an iterator that combines the old and new EventGraph timelines
    // This TemporalEventSource only keeps modifications of EventGraphs from the oldTemporalEventSource.

    // The idea is to get a combined commitsByTime map rolling up the nested commitsByTime members of
    // TemporalEventSource.  Then, convert that into sequence of TimePoints.  However, this iterator
    // may be constructed (and possibly used) before commitsByTime has been filled by the simulation.
    // This allows us to use this iterator to stream information during simulation to pipeline computation.
    // Thus, we provide an iterator (TreeMapIterator) that works for a growing map of commitsByTime.
    // So, instead of combining maps, we need to combine iterators.  But, we can simplify this by
    // assuming that the simulation is complete in the oldTemporalEventSource, and can combine those
    // old nested commitsByTime with oldTemporalEventSource.getCombinedCommitsByTime().  Then we
    // can combine the iterators of the old and new commitsByTime, and convert that iterator into one
    // that generates TimePoints instead of map entries.
    Iterator<Map.Entry<Duration, List <TimePoint.Commit>>> treeMapIter;
    var i1 = new TreeMapIterator<>(commitsByTime);
    if (oldTemporalEventSource == null) {
      treeMapIter = i1;
    } else {
      var m = oldTemporalEventSource.getCombinedCommitsByTime();
      var i2 = m.entrySet().iterator();
      treeMapIter = new CombinedTreeMapIterator<>(i1, i2, (list1, list2) -> list1);
    }
    var i3 = new TimePointIteratorFromCommitMap(treeMapIter);
    return i3;
  }

  public void setTopicStale(Topic<?> topic, Duration offsetTime) {
    if (debug) System.out.println("setTopicStale(" + topic + ", " + offsetTime + ")");
    staleTopics.computeIfAbsent(topic, $ -> new TreeMap<>()).put(offsetTime, true);
  }

  public void setTopicUnstale(Topic<?> topic, Duration offsetTime) {
    if (debug) System.out.println("setTopicUnStale(" + topic + ", " + offsetTime + ")");
    staleTopics.computeIfAbsent(topic, $ -> new TreeMap<>()).put(offsetTime, false);
  }

  /**
   * Determine whether a topic been marked stale at a specified time.
   * @param topic topic to check for staleness
   * @param timeOffset the staleness time
   * @return true if the topic is marked stale at timeOffset
   */
  public boolean isTopicStale(Topic<?> topic, Duration timeOffset) {
    if (oldTemporalEventSource == null) return true;
    var map = this.staleTopics.get(topic);
    if (map == null) return false;
    final Duration staleTime = map.floorKey(timeOffset);
    return staleTime != null && map.get(staleTime);
  }

  public Optional<Duration> whenIsTopicStale(Topic<?> topic, Duration earliestTimeOffset, Duration latestTimeOffset) {
    if (oldTemporalEventSource == null) return Optional.of(earliestTimeOffset);
    var map = this.staleTopics.get(topic);
    if (map == null) return Optional.empty();
    final Duration staleTime = map.floorKey(earliestTimeOffset);
    if (staleTime != null && map.get(staleTime)) {
      return Optional.of(earliestTimeOffset);
    }
    var submap = map.subMap(earliestTimeOffset, true, latestTimeOffset, true);
    for (Map.Entry<Duration, Boolean> e : submap.entrySet()) {
      if (e.getValue()) return Optional.of(e.getKey());
    }
    return Optional.empty();
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
   * @param endTime the time to which the cell is stepped
   * @param includeEndTime whether to apply the Events occurring at endTime
   */
  public void stepUpSimple(final Cell<?> cell, final Duration endTime, final boolean includeEndTime) {
    if (debug) System.out.println("stepUpSimple(" + cell + ", " + endTime + ", " + includeEndTime + ") BEGIN");
    final NavigableMap<Duration, List<EventGraph<Event>>> subTimeline;
    var cellTimePair = getCellTime(cell);
    var cellTime = cellTimePair.getLeft();
    var cellSteppedAtTime = cellTimePair.getRight();
    if (cellTime.longerThan(endTime)) {
      throw new UnsupportedOperationException("Trying to step cell from the past");
    }
    try {
      final TreeMap<Duration, List<EventGraph<Event>>> eventsByTimeForTopic = eventsByTopic.get(cell.getTopic());
      if (eventsByTimeForTopic == null) {
        if (endTime.longerThan(cellTime) && endTime.shorterThan(Duration.MAX_VALUE)) {
          if (debug) System.out.println("cell.step(" + endTime.minus(cellTime) + ")");
          cell.step(endTime.minus(cellTime));
          cellTime = endTime;
          cellSteppedAtTime = 0;
          putCellTime(cell, cellTime, cellSteppedAtTime);
        }
        if (debug) System.out.println("stepUpSimple(" + cell + ", " + endTime + ", " + includeEndTime + ") END");
        return;
      }
      subTimeline = eventsByTimeForTopic.subMap(cellTime, true, endTime, includeEndTime);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    for (Entry<Duration, List<EventGraph<Event>>> e : subTimeline.entrySet()) {
      final List<EventGraph<Event>> eventGraphList = e.getValue();
      var delta = e.getKey().minus(cellTime);
      if (delta.isPositive()) {
        if (debug) System.out.println("cell.step(" + delta + ")");
        cell.step(delta);
        cellTime = e.getKey();
        cellSteppedAtTime = 0;
        putCellTime(cell, cellTime, cellSteppedAtTime);
      } else if (delta.isNegative()) {
        throw new UnsupportedOperationException("Trying to step cell from the past");
      }
//      cellTimePair = getCellTime(cell);
      if (cellTime.isEqualTo(e.getKey()) && cellSteppedAtTime == eventGraphList.size()) {
        // We've already applied all graphs; not doing it twice!
      } else {
        for (; cellSteppedAtTime < eventGraphList.size(); ++cellSteppedAtTime) {
          var eventGraph = eventGraphList.get(cellSteppedAtTime);
          if (debug) System.out.println("cell.apply(" + eventGraph + ")");
          cell.apply(eventGraph, null, false);
        }
        cellTime = e.getKey();
        putCellTime(cell, cellTime, cellSteppedAtTime);
      }
    }
    if (endTime.longerThan(cellTime) && endTime.shorterThan(Duration.MAX_VALUE)) {
      if (debug) System.out.println("cell.step(" + endTime.minus(cellTime) + ")");
      cell.step(endTime.minus(cellTime));
      putCellTime(cell, endTime, 0);
    }
    if (debug) System.out.println("stepUpSimple(" + cell + ", " + endTime + ", " + includeEndTime + ") END");
  }

  /**
   * Step up the Cell through the timeline of EventGraphs.  Stepping up means to
   * apply Effects from Events up to some point in time.
   *
   * @param cell the Cell to step up
   * @param endTime the time up to which the cell is stepped
   * @param includeEndTime whether to apply the Events occurring at endTime
   */
  public void stepUp(final Cell<?> cell, final Duration endTime, final boolean includeEndTime) {
    // Separate out the simpler case of no past simulation for readability
    if (oldTemporalEventSource == null) {
      stepUpSimple(cell, endTime, includeEndTime);
      return;
    }

    // Get the relevant submap of EventGraphs for both the old and new timelines.
    final NavigableMap<Duration, List<EventGraph<Event>>> subTimeline;
    NavigableMap<Duration, List<EventGraph<Event>>> oldSubTimeline;
    var cellTimePair = getCellTime(cell);
    var cellTime = cellTimePair.getLeft();
    final var originalCellTime = cellTime;
    var cellSteppedAtTime = cellTimePair.getRight();
    final var originalCellSteppedAtTime = cellSteppedAtTime;
    if (cellTime.longerThan(endTime)) {
      throw new UnsupportedOperationException("Trying to step cell from the past.");
    }
    final TreeMap<Duration, List<EventGraph<Event>>> mo;
    try {
      var t = cell.getTopic();
      var m = eventsByTopic.get(t);
      subTimeline = m == null ? null : m.subMap(cellTime, true, endTime, includeEndTime);
      mo = oldTemporalEventSource.getCombinedEventsByTopic().get(t);
      oldSubTimeline = mo == null ? null : mo.subMap(cellTime, true, endTime, includeEndTime);
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
    if (debug) System.out.println("" + i + " stepUp(" + cell.getTopic() + ", " + endTime + ", " + includeEndTime + "): cellState = " + cell.toString() + ", stale = " + stale + ", cellTime = " + cellTimePair + ", oldState = " + oldCell.getState().toString() + ", oldCellTime = " + oldCellTimePair);

    // Each iteration of this loop processes a time delta and a list of EventGraphs; else just steps up to endTime.
    // The cell applies both the old and new EventGraphs except only the new when at the same timepoint.
    // An old cell is created and/or stepped just within the old TemporalEventSource to determine if the
    // new cell becomes stale or unstale.  The old cell is abandoned when not stale and when there are no
    // new EventGraphs, which are just changes (additions and replacements) on top of the old.
    int done = 0;
    while (done < 2) {
      boolean stepped = false;

      // step(timeDelta) for oldCell if necessary
      if (stale) {  // Only step if the topic is stale
        var minWrtOld = Duration.min(entryTime, oldEntryTime, endTime);
        if (oldCellTime.shorterThan(minWrtOld) && minWrtOld.shorterThan(Duration.MAX_VALUE)) {
          stepped = true;
          oldCell.step(minWrtOld.minus(oldCellTime));
          if (debug) System.out.println("" + i + " stepUp(): oldCell.step(minWrtOld=" + minWrtOld + " - oldCellTime=" + oldCellTime + " = " + minWrtOld.minus(oldCellTime) + ")");
          oldCellTime = minWrtOld;
          oldCellSteppedAtTime = 0;
          oldTemporalEventSource.putCellTime(oldCell, oldCellTime, oldCellSteppedAtTime);
        }
      }
      // step(timeDelta) for new cell if necessary
      var minWrtNew = Duration.min(entryTime, oldEntryTime, endTime);
      if (cellTime.shorterThan(minWrtNew) && minWrtNew.shorterThan(Duration.MAX_VALUE)) {
        stepped = true;
        cell.step(minWrtNew.minus(cellTime));
        if (debug) System.out.println("" + i + " stepUp(): cell.step(minWrtOld=" + minWrtNew + " - cellTime=" + cellTime + " = " + minWrtNew.minus(cellTime) + ")");
        cellTime = minWrtNew;
        cellSteppedAtTime = 0;
      }

      // check staleness
      boolean timesAreEqual = stale && cellTime.isEqualTo(oldCellTime) && cellSteppedAtTime.equals(oldCellSteppedAtTime); // inserted stale thinking it would be faster to skip isEqualTo()
      if (debug) System.out.println("" + i + " stepUp(): timesAreEqual = " + timesAreEqual);

      if (stale && stepped && timesAreEqual) {
        stale = updateStale(cell, oldCell);
      }

      // Apply old EventGraph
      boolean oldCellStateChanged = false;
      boolean cellStateChanged = false;
      if (oldEntry != null &&
          oldEntryTime.isEqualTo(cellTime) &&
          (oldCellTime.shorterThan(endTime) || (includeEndTime && oldCellTime.isEqualTo(endTime)))) {
        var unequalGraphs = entry != null && entryTime.isEqualTo(oldEntryTime) && !oldEntry.getValue().equals(entry.getValue());

        // Step old cell if stale or if the new EventGraph is changed
        var oldEventGraphList = oldEntry.getValue();
        if (stale || unequalGraphs) {
          // If topic is not stale, and old cell is not stepped up, then it was abandoned, and need to create a new one.
          if (!stale && unequalGraphs && !oldCellTime.isEqualTo(cellTime)) {
            //cellCache.computeIfAbsent(cell.getTopic(), $ -> new TreeMap<>()).put(oldCellTime, oldCell);
            if (debug) System.out.println("" + i + " stepUp(): oldCell = cell.duplicate()");
            oldCell = cell.duplicate();  // Would stepping up old cell be faster in some cases?
            oldCellTime = cellTime;
            oldCellSteppedAtTime = cellSteppedAtTime;
            oldCellStateChanged = true;
//            oldSubTimeline = mo == null ? null : mo.subMap(cellTime, true, endTime, includeEndTime);
//            oldIter = oldSubTimeline == null ? null : oldSubTimeline.entrySet().iterator();
//            oldEntry = oldIter != null && oldIter.hasNext() ? oldIter.next() : null;
//            oldEntryTime = oldEntry == null ? Duration.MAX_VALUE : oldEntry.getKey();
          }
          final var oldOldState = oldCell.getState(); // getState() generates a copy, so oldState won't change
          if (oldCellSteppedAtTime < oldEventGraphList.size() &&
              (!originalOldCellTime.isEqualTo(oldCellTime) || originalOldCellStoppedAtTime < oldEventGraphList.size())) {
            for (; oldCellSteppedAtTime < oldEventGraphList.size(); ++oldCellSteppedAtTime) {
              var eventGraph = oldEventGraphList.get(oldCellSteppedAtTime);
              oldCell.apply(eventGraph, null, false);
              if (debug) System.out.println("" + i + " stepUp(): oldCell.apply(oldGraph: " + eventGraph + ") oldCellState = " + oldCell);
            }
          }
          oldTemporalEventSource.putCellTime(oldCell, oldCellTime, oldCellSteppedAtTime);
          oldCellStateChanged = oldCellStateChanged || !oldCell.getState().equals(oldOldState);
        }

        // Step up new cell if no new EventGraph at this time.
        if (entry == null || entryTime.longerThan(oldEntryTime)) {
          final var oldState = cell.getState(); // getState() generates a copy, so oldState won't change
          if (!originalCellTime.isEqualTo(cellTime) || originalCellSteppedAtTime < oldEventGraphList.size()) {
            for (; cellSteppedAtTime < oldEventGraphList.size(); ++cellSteppedAtTime) {
              var eventGraph = oldEventGraphList.get(cellSteppedAtTime);
              cell.apply(eventGraph, null, false);
              if (debug) System.out.println("" + i + " stepUp(): cell.apply(oldGraph: " + eventGraph + ") cellState = " + cell);
            }
          }
          cellStateChanged = !cell.getState().equals(oldState);
        }
        oldEntry = oldIter != null && oldIter.hasNext() ? oldIter.next() : null;
        oldEntryTime = oldEntry == null ? Duration.MAX_VALUE : oldEntry.getKey();
        if (debug) System.out.println("" + i + " stepUp(): oldEntry = " + oldEntry + ", oldEntryTime = " + oldEntryTime);
      }

      // Apply new EventGraph
      if (entry != null && entryTime.isEqualTo(cellTime) &&
          (cellTime.shorterThan(endTime) || (includeEndTime && cellTime.isEqualTo(endTime)))) {
        final var newEventGraphList = entry.getValue();
        final var oldState = cell.getState(); // getState() generates a copy, so oldState won't change
        if (cellSteppedAtTime < newEventGraphList.size() &&
            (!originalCellTime.isEqualTo(cellTime) || originalCellSteppedAtTime < newEventGraphList.size())) {
          for (; cellSteppedAtTime < newEventGraphList.size(); ++cellSteppedAtTime) {
            var eventGraph = newEventGraphList.get(cellSteppedAtTime);
            cell.apply(eventGraph, null, false);
            if (debug) System.out.println("" + i + " stepUp(): cell.apply(newGraph: " + eventGraph + ") cellState = " + cell);
          }
        }
        cellStateChanged = !cell.getState().equals(oldState);
        entry = iter != null && iter.hasNext() ? iter.next() : null;
        entryTime = entry == null ? Duration.MAX_VALUE : entry.getKey();
        if (debug) System.out.println("" + i + " stepUp(): entry = " + entry + ", entryTime = " + entryTime);
      }

      // check staleness
      if (timesAreEqual && (stale || cellStateChanged || oldCellStateChanged)) {
        stale = updateStale(cell, oldCell);
      }
      if ( !( (cellTime.shorterThan(endTime) || (stale && oldCellTime.shorterThan(endTime))) &&
              (entry != null || oldEntry != null) ) ) {
        ++done;
      }

    }
    putCellTime(cell, cellTime, cellSteppedAtTime);
    if (debug) cellTimePair = Pair.of(cellTime, cellSteppedAtTime);
    if (debug) System.out.println("" + i + " END stepUp(" + cell.getTopic() + ", " + endTime + ", " + includeEndTime + "): cellState = " + cell.toString() + ", stale = " + stale + ", cellTime = " + cellTimePair);
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

  public <State> Cell<State> getCell(Topic<?> topic, Duration endTime, boolean includeEndTime) {
    Optional<LiveCell<?>> cell = liveCells.getCells(topic).stream().findFirst();
    if (cell.isEmpty()) {
      throw new RuntimeException("Can't find cell for query.");
    }
    return getCell((Cell<State>)cell.get().get(), endTime, includeEndTime);
  }

  public <State> Cell<State> getCell(Cell<State> cell, Duration endTime, boolean includeEndTime) {
    var time = getCellTime(cell).getLeft();
    // Use the one in LiveCells if not asking for a time in the past.
    if (time == null || time.noLongerThan(endTime)) {
      stepUp(cell, endTime, includeEndTime);
      return cell;
    }
    // For a cell in the past, use the cell cache
    Cell<State> pastCell = getOrCreateCellInCache(cell.getTopic(), endTime, includeEndTime);
    return pastCell;
  }

  public <State> Cell<State> getCell(Query<State> query, Duration endTime, boolean includeEndTime) {
    Optional<LiveCell<State>> cell = liveCells.getLiveCell(query);
    if (cell.isEmpty()) {
      throw new RuntimeException("Can't find cell for query.");
    }
    return getCell(cell.get().get(), endTime, includeEndTime);
  }

  public <State> Cell<State> getOrCreateCellInCache(Topic<?> topic, Duration endTime, boolean includeEndTime) {
    final TreeMap<Duration, Cell<?>> inner = cellCache.computeIfAbsent(topic, $ -> new TreeMap<>());
    final Entry<Duration, Cell<?>> entry = inner.floorEntry(endTime);
    Cell<?> cell;
    if (entry != null) {
      cell = entry.getValue();
      // TODO: maybe pass in boolean for whether to duplicate the cell in the cache instead of removing and adding back after stepping up
      inner.remove(entry.getKey());
    } else {
      cell = missionModel.getInitialCells().getCells(topic).stream().findFirst().orElseThrow().get().duplicate();
    }
    stepUp(cell, endTime, includeEndTime);
    inner.put(endTime, cell);
    return (Cell<State>)cell; // TODO: avoid this force cast and associated compiler warning
  }

  public Optional<LiveCell<?>> getOldCell(LiveCell<?> cell) {
    if (oldTemporalEventSource == null) return Optional.empty();
    return oldTemporalEventSource.liveCells.getCells(cell.get().getTopic()).stream().findFirst();
  }

  public Optional<Cell<?>> getOldCell(Cell<?> cell) {
    if (oldTemporalEventSource == null) return Optional.empty();
    return oldTemporalEventSource.liveCells.getCells(cell.getTopic()).stream().findFirst().map(lc -> lc.cell);
  }

  public Pair<Duration, Integer> getCellTime(Cell<?> cell) {
    var cellTime = cellTimes.get(cell);
    if (cellTime == null) {
      return Pair.of(Duration.ZERO, 0);
    }
    Integer cellStepped = this.cellTimeStepped.get(cell);
    if (cellStepped == null) {
      this.cellTimeStepped.put(cell, 0);
      cellStepped = 0;
    }
    return Pair.of(cellTime, cellStepped);
  }

  public void putCellTime(Cell<?> cell, Duration cellTime, int cellStepped) {
    this.cellTimes.put(cell, cellTime);
    this.cellTimeStepped.put(cell, cellStepped);
  }


  @Override
  public TemporalCursor cursor() {
    return new TemporalCursor();
  }

  public final class TemporalCursor implements Cursor {
//    private final Iterator<TimePoint> iterator;

//    TemporalCursor(Iterator<TimePoint> iterator) {
//      this.iterator = iterator;
//    }

    private TemporalCursor() {
//      this(TemporalEventSource.this.iterator());
    }

    @Override
    public void stepUp(final Cell<?> cell) {
      TemporalEventSource.this.stepUp(cell, curTime(), true);
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

  public static void extractTopics(final Set<Topic<?>> accumulator, EventGraph<Event> graph) {
    extractTopics(accumulator, graph, null);
  }
  public static void extractTopics(final Set<Topic<?>> accumulator, EventGraph<Event> graph, Predicate<Event> p) {
    while (true) {
      if (graph instanceof EventGraph.Empty) {
        // There are no events here!
        return;
      } else if (graph instanceof EventGraph.Atom<Event> g) {
        if(p == null || p.test(g.atom())) {
          accumulator.add(g.atom().topic());
        }
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
