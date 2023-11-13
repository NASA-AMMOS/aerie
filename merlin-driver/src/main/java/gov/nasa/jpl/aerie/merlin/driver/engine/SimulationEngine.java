package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.CombinedSimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.EventGraphFlattener;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel.SerializableTopic;
import gov.nasa.jpl.aerie.merlin.driver.ResourceTracker;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivityId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResultsInterface;
import gov.nasa.jpl.aerie.merlin.driver.UnfinishedActivity;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Cell;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Event;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A representation of the work remaining to do during a simulation, and its accumulated results.
 */
public final class SimulationEngine implements AutoCloseable {
  private static boolean debug = true;
  private static boolean trace = false;

  /** The engine from a previous simulation, which we will leverage to avoid redundant computation */
  public final SimulationEngine oldEngine;

  /** The EventGraphs separated by Durations between the events */
  public final TemporalEventSource timeline;
  private LiveCells cells;
  /** The set of all jobs waiting for time to pass. */
  private final JobSchedule<JobId, SchedulingInstant> scheduledJobs = new JobSchedule<>();
  /** The set of all jobs waiting on a given signal. */
  private final Subscriptions<SignalId, TaskId> waitingTasks = new Subscriptions<>();
  /** The set of conditions depending on a given set of topics. */
  private final Subscriptions<Topic<?>, ConditionId> waitingConditions = new Subscriptions<>();
  /** The set of queries depending on a given set of topics. */
  private final Subscriptions<Topic<?>, ResourceId> waitingResources = new Subscriptions<>();
  /** The topics referenced (cells read) by the last computation of the resource. */
  private HashMap<ResourceId, Set<Topic<?>>> referencedTopics = new HashMap<>();
  /** Separates generation of resource profile results from other parts of the simulation */
  public ResourceTracker resourceTracker;
  /** The history of when tasks read topics/cells */
  private final HashMap<Topic<?>, TreeMap<Duration, HashMap<TaskId, Event>>> cellReadHistory = new HashMap<>();
  private final TreeMap<Duration, HashSet<TaskId>> removedCellReadHistory = new TreeMap<>();

  private final MissionModel<?> missionModel;

  /** The start time of the simulation, from which other times are offsets */
  private final Instant startTime;

  /**
   * Counts from 0 the commits/steps at the same timepoint in order to align events of re-executed tasks
   */
  private int stepIndexAtTime = 0;
  /**
   * Whether we are adding events concurrent with existing events.
   */
  private boolean overlayingEvents = false;

  public Map<ActivityDirectiveId, ActivityDirective> scheduledDirectives = null;
  public Map<String, Map<ActivityDirectiveId, ActivityDirective>> directivesDiff = null;

  public final TaskInfo taskInfo = new TaskInfo();
//  private Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> realProfiles = new HashMap<>();
//  private Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> discreteProfiles = new HashMap<>();
  private final HashMap<SimulatedActivityId, SimulatedActivity> simulatedActivities = new HashMap<>();
  private final Set<SimulatedActivityId> removedActivities = new HashSet<>();
  private final HashMap<SimulatedActivityId, UnfinishedActivity> unfinishedActivities = new HashMap<>();
  private final SortedMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> serializedTimeline = new TreeMap<>();
  private final List<Triple<Integer, String, ValueSchema>> topics = new ArrayList<>();
  private SimulationResults simulationResults = null;
  public static final Topic<ActivityDirectiveId> defaultActivityTopic = new Topic<>();
  private HashMap<String, SimulatedActivityId> taskToSimulatedActivityId = null;

  public SimulationEngine(Instant startTime, MissionModel<?> missionModel, SimulationEngine oldEngine,
                          final ResourceTracker resourceTracker) {

    this.startTime = startTime;
    this.missionModel = missionModel;
    this.oldEngine = oldEngine;
    this.resourceTracker = resourceTracker;
    this.timeline = new TemporalEventSource(null, missionModel,
                                            oldEngine == null ? null : oldEngine.timeline);
    if (oldEngine != null) {
      oldEngine.cells = new LiveCells(oldEngine.timeline, oldEngine.missionModel.getInitialCells());
      this.cells = new LiveCells(timeline, oldEngine.missionModel.getInitialCells());  // HACK: good for in-memory but with DB or difft mission model configuration,...
      //this.defaultActivityTopic = oldEngine.defaultActivityTopic;
    } else {
      this.cells = new LiveCells(timeline, missionModel.getInitialCells());
      //this.defaultActivityTopic = new Topic<>();
    }
    this.timeline.liveCells = this.cells;
    if (debug) System.out.println("new SimulationEngine(startTime=" + startTime + ")");
  }

  /** When tasks become stale */
  private final Map<TaskId, Duration> staleTasks = new HashMap<>();

  /** The execution state for every task. */
  private final Map<TaskId, ExecutionState<?>> tasks = new HashMap<>();
  /** Remember the TaskFactory for each task so that we can re-run it */
  private final Map<TaskId, TaskFactory<?>> taskFactories = new HashMap<>();
  private final Map<TaskFactory<?>, TaskId> taskIdsForFactories = new HashMap<>();
  /** Remember which tasks were daemon-spawned */
  private final Set<TaskId> daemonTasks = new HashSet<>();
  /** The getter for each tracked condition. */
  private final Map<ConditionId, Condition> conditions = new HashMap<>();
  /** The profiling state for each tracked resource. */
  private final Map<ResourceId, ProfilingState<?>> resources = new HashMap<>();

  /** The task that spawned a given task (if any). */
  private final Map<TaskId, TaskId> taskParent = new HashMap<>();
  /** The set of children for each task (if any). */
  @DerivedFrom("taskParent")
  private final Map<TaskId, Set<TaskId>> taskChildren = new HashMap<>();

  /** A thread pool that modeled tasks can use to keep track of their state between steps. */
  private final ExecutorService executor = getLoomOrFallback();

  /**  */
  public void putInCellReadHistory(Topic<?> topic, TaskId taskId, Event noop, Duration time) {
    // TODO: Can't we just get this from eventsByTopic instead of having a separate data structure?
    var inner = cellReadHistory.computeIfAbsent(topic, $ -> new TreeMap<>());
    inner.computeIfAbsent(time, $ -> new HashMap<>()).put(taskId, noop);
  }

  /**
   * A cache of the combinedHistory so that it does not need to be recomputed after simulation.  The parent engine sets
   * the cache for the child engine per topic and clears it for the grandchild per topic.  This assumes that an engine
   * will not have more than one parent.
   */
  protected HashMap<Topic<?>, TreeMap<Duration, HashMap<TaskId, Event>>> _combinedHistory = new HashMap<>();
  /**
   * A cache of part of the combinedHistory computation that is the old combined history without the removed task history.
   * This should be cleared by the parent engine.
   */
  protected HashMap<Topic<?>, TreeMap<Duration, HashMap<TaskId, Event>>> _oldCleanedHistory = new HashMap<>();
  // protected Duration _combinedHistoryTime = null;

//  public HashMap<Topic<?>, TreeMap<Duration, HashMap<TaskId, Event>>> getCombinedCellReadHistory() {
//
//  }
//  public TreeMap<Duration, HashMap<TaskId, Event>> getCombinedCellReadHistory(Topic<?> topic) {
//    return getCombinedCellReadHistory().get(topic);
//  }

  public TreeMap<Duration, HashMap<TaskId, Event>> getCombinedCellReadHistory(Topic<?> topic) {
    // check cache
    var inner = _combinedHistory.get(topic);
    if (inner != null) return inner;

    inner = cellReadHistory.get(topic);
    if (oldEngine == null) {
      // If there's no history from an old engine, then just set the cache to the local history
      _combinedHistory = cellReadHistory;
      if (inner == null) return new TreeMap();
      return inner;
    }

    var oldInner = oldEngine.getCombinedCellReadHistory(topic);
    if (oldEngine._combinedHistory.get(topic) == null) {
      oldEngine._combinedHistory.put(topic, oldInner);
      if (oldEngine.oldEngine != null && oldEngine.oldEngine._combinedHistory != null) {
        oldEngine.oldEngine._combinedHistory.remove(topic);
        oldEngine.oldEngine._oldCleanedHistory.remove(topic);
        oldEngine.oldEngine.cellReadHistory.remove(topic);
      }
    }

    // Clean the removed tasks from the old read history
    // Check for cached computation first
    var oldCleanedHistory = _oldCleanedHistory.get(topic);
    if (oldCleanedHistory == null) {
      //TreeMap<Duration, HashMap<TaskId, Event>> oldCleanedHistory = null;
      Set<Duration> commonKeys = oldInner.keySet().stream().filter(d -> removedCellReadHistory.containsKey(d)).collect(
          Collectors.toSet());
      if (commonKeys.isEmpty()) {
        oldCleanedHistory = oldInner;
      } else {
        oldCleanedHistory = new TreeMap<>();
        for (var oDur : commonKeys) {
          var rTasks = removedCellReadHistory.get(oDur);
          var oTaskMap = oldInner.get(oDur);
          if (rTasks == null) {
            oldCleanedHistory.put(oDur, oTaskMap);
          }
          HashMap<TaskId, Event> cleanTaskMap = new HashMap<>();
          Set<TaskId> commonTasks = oTaskMap.keySet().stream().filter(t -> rTasks.contains(t)).collect(
              Collectors.toSet());
          if (commonTasks.isEmpty()) {
            cleanTaskMap = oTaskMap;
          } else {
            cleanTaskMap = new HashMap<>();
            for (var tEntry : oTaskMap.entrySet()) {
              var oTaskId = tEntry.getKey();
              if (!rTasks.contains(oTaskId)) {
                cleanTaskMap.put(tEntry.getKey(), tEntry.getValue());
              }
            }
          }
          oldCleanedHistory.put(oDur, cleanTaskMap);
        }
      }
      // Now cache the results
      _oldCleanedHistory.put(topic, oldCleanedHistory);
    }

    // Now merge local history with old cleaned history
    TreeMap<Duration, HashMap<TaskId, Event>> combinedTopicHistory = null;
    if (oldCleanedHistory.isEmpty()) {
      combinedTopicHistory = inner;
    } else if (inner == null || inner.isEmpty()) {
      combinedTopicHistory = oldCleanedHistory;
    }

    // No need to cache this.  The parent engine caches this.
    return combinedTopicHistory;
  }



  /**
   * Get the earliest time within a specified range that potentially stale cells are read by tasks not scheduled
   * to be re-run.
   * @param after start of time range
   * @param before end of time range
   * @return the time of the earliest read, the tasks doing the reads, and the noop Events/Topics read by each task
   */
  /** Get the earliest time that topics become stale and return those topics with the time */
  public Pair<Duration, Map<TaskId, HashSet<Pair<Topic<?>, Event>>>> earliestStaleReadsNew(Duration after, Duration before, Topic<Topic<?>> queryTopic) {
    // We need to have the reads sorted according to the event graph.  Currently, this function doesn't
    // handle a task reading a cell more than once in a graph.  But, we should make sure we handle this case. TODO
    var earliest = before;
    final var tasks = new HashMap<TaskId, HashSet<Pair<Topic<?>, Event>>>();
    ConcurrentSkipListSet<Duration> durs = timeline.staleTopics.entrySet().stream().collect(() -> new ConcurrentSkipListSet<Duration>(),
                                                                                            (set, entry) -> set.addAll(entry.getValue().keySet().stream().filter(d -> entry.getValue().get(d)).toList()),
                                                                                            (set1, set2) -> set1.addAll(set2));
    if (durs.isEmpty()) return Pair.of(Duration.MAX_VALUE, Collections.emptyMap());
    var earliestStaleTopic = durs.higher(after);
    final TreeMap<Duration, List<EventGraph<Event>>> readEvents = oldEngine.timeline.getCombinedEventsByTopic().get(queryTopic);
    if (readEvents == null || readEvents.isEmpty()) return Pair.of(Duration.MAX_VALUE, Collections.emptyMap());
    var readEventsSubmap = readEvents.subMap(after, false, before, true);
    for (var te : readEventsSubmap.entrySet()) {
      final List<EventGraph<Event>> graphList = te.getValue();
      for (var eventGraph : graphList) {
        final List<Pair<String, Event>> flatGraph = EventGraphFlattener.flatten(eventGraph);
        for (var pair : flatGraph) {
          Event event = pair.getRight();
          // HERE!
        }
      }
    }

    if (readEvents.isEmpty()) return Pair.of( Duration.MAX_VALUE, Collections.emptyMap());
    for (var entry : timeline.staleTopics.entrySet()) {
      Topic<?> topic = entry.getKey();
      var subMap = entry.getValue().subMap(after, false, earliest, true);
      Duration d = null;
      for (var e : subMap.entrySet()) {
        if (e.getValue()) {
          d = e.getKey();
          var topicEventsSubMap = readEventsSubmap.subMap(d, true, earliest, true);
          break;
        }
      }
      if (d == null) {
        continue;
      }
      int comp = d.compareTo(earliest);
      if (comp <= 0) {
        if (comp < 0) tasks.clear();
        //tasks.add(topic);
        earliest = d;
      }
    }
    if (tasks.isEmpty()) earliest = Duration.MAX_VALUE;
    return Pair.of(earliest, tasks);
  }

//public String whatsThis(Topic<?> topic) {
//    return missionModel.getResources().entrySet().stream().filter(e -> e.getValue().toString()).findFirst()
//}


  public Pair<Duration, Map<TaskId, HashSet<Pair<Topic<?>, Event>>>> earliestStaleReads(Duration after, Duration before) {
    // We need to have the reads sorted according to the event graph.  Currently, this function doesn't
    // handle a task reading a cell more than once in a graph.  But, we should make sure we handle this case. TODO
    var earliest = before;
    final var tasks = new HashMap<TaskId, HashSet<Pair<Topic<?>, Event>>>();
    final var topicsStale = timeline.staleTopics.keySet();
    for (var topic : topicsStale) {
      var topicReads = getCombinedCellReadHistory(topic);
      if (topicReads == null || topicReads.isEmpty()) {
        continue;
      }
      final NavigableMap<Duration, HashMap<TaskId, Event>> topicReadsAfter =
          topicReads.subMap(after, false, earliest, true);
      if (topicReadsAfter == null || topicReadsAfter.isEmpty()) {
        continue;
      }
      for (var entry : topicReadsAfter.entrySet()) {
        Duration d = entry.getKey();
        HashMap<TaskId, Event> taskIds = entry.getValue();
//        // filter out tasks of removed activities
//        // Moved and removed activities have
//        var filteredStream = entry.getValue().entrySet().stream().filter(e -> !removedActivities.contains(e.getKey()) &&
//                                                                              !(oldEngine.getSimulatedActivityIdForTaskId(e.getKey()) != null &&
//                                                                                removedActivities.contains(oldEngine.getSimulatedActivityIdForTaskId(e.getKey()))));
//        HashMap<TaskId, Event> taskIds = filteredStream.collect(() -> new HashMap<TaskId, Event>(),
//                                                                (map, e) -> map.put(e.getKey(), e.getValue()),
//                                                                (map1, map2) -> map1.putAll(map2));
        if (timeline.isTopicStale(topic, d)) {
          if (d.shorterThan(earliest)) {
            earliest = d;
            tasks.clear();
          } else if (d.longerThan(earliest)) {
            if (!tasks.isEmpty()) break;
            continue;
          }
          taskIds.forEach((id, event) -> tasks.computeIfAbsent(id, $ -> new HashSet<>()).add(Pair.of(topic, event)));
        }
      }
    }
    if (tasks.isEmpty()) earliest = Duration.MAX_VALUE;
    return Pair.of(earliest, tasks);
  }

  /**
   * Get the earliest time that stale topics have events in the old simulation.  These are places where we need
   * to update resource profiles but that aren't captured by {@link #earliestStaleTopics(Duration, Duration)}.
   */
  public Pair<List<Topic<?>>, Duration> nextStaleTopicOldEvents(Duration after, Duration before) {
    var list = new ArrayList<Topic<?>>();
    Duration earliest = before;
    for (var entry : timeline.staleTopics.entrySet()) {
      Topic<?> topic = entry.getKey();
      Optional<Duration> nextStale = timeline.whenIsTopicStale(topic, after, before);
      if (nextStale.isEmpty()) continue;
      TreeMap<Duration, List<EventGraph<Event>>> eventsByTime =
          timeline.oldTemporalEventSource.getCombinedEventsByTopic().get(topic);
      if (eventsByTime == null) continue;
      var subMap = eventsByTime.subMap(nextStale.get(), !nextStale.get().isEqualTo(after), earliest, true);
      Duration d = null;
      for (var e : subMap.entrySet()) {
        final List<EventGraph<Event>> events = e.getValue();
        if (events == null || events.isEmpty()) continue;
        boolean affectsTopic = events.stream().anyMatch(graph -> Optional.ofNullable(timeline.oldTemporalEventSource.topicsForEventGraph.get(graph)).map(topics -> topics.contains(topic)).orElse(false));
        if (!affectsTopic) continue;  // This is the case where old events were removed.
        d = e.getKey();
        if (!timeline.isTopicStale(topic, d)) continue;
        break;
      }
      if (d == null) {
        continue;
      }
      int comp = d.compareTo(earliest);
      if (comp <= 0) {
        if (comp < 0) list.clear();
        list.add(topic);
        earliest = d;
      }
    }
    if (list.isEmpty()) earliest = Duration.MAX_VALUE;
    return Pair.of(list, earliest);
  }

  /** Get the earliest time that topics become stale and return those topics with the time */
  public Pair<List<Topic<?>>, Duration> earliestStaleTopics(Duration after, Duration before) {
    var list = new ArrayList<Topic<?>>();
    Duration earliest = before;
    for (var entry : timeline.staleTopics.entrySet()) {
      Topic<?> topic = entry.getKey();
      var subMap = entry.getValue().subMap(after, false, earliest, true);
      Duration d = null;
      for (var e : subMap.entrySet()) {
        if (e.getValue()) {
          d = e.getKey();
          break;
        }
      }
      if (d == null) {
        continue;
      }
      int comp = d.compareTo(earliest);
      if (comp <= 0) {
        if (comp < 0) list.clear();
        list.add(topic);
        earliest = d;
      }
    }
    if (list.isEmpty()) earliest = Duration.MAX_VALUE;
    return Pair.of(list, earliest);
  }

  public Pair<List<Topic<?>>, Duration> earliestConditionTopics(Duration after, Duration before) {
    var list = new ArrayList<Topic<?>>();
    Duration earliest = before;
    for (Topic topic : this.waitingConditions.getTopics()) {
      TreeMap<Duration, List<EventGraph<Event>>> eventsByTime =
          timeline.getCombinedEventsByTopic().get(topic);
      if (eventsByTime == null) continue;
      var subMap = eventsByTime.subMap(after, false, earliest, true);
      Duration d = null;
      for (var e : subMap.entrySet()) {
        final List<EventGraph<Event>> events = e.getValue();
        if (events == null || events.isEmpty()) continue;
//        boolean affectsTopic = events.stream().anyMatch(graph -> Optional.ofNullable(timeline.oldTemporalEventSource.topicsForEventGraph.get(graph)).map(topics -> topics.contains(topic)).orElse(false));
//        if (!affectsTopic) continue;  // This is the case where old events were removed.
        d = e.getKey();
        break;
      }
      if (d == null) {
        continue;
      }
      int comp = d.compareTo(earliest);
      if (comp <= 0) {
        if (comp < 0) list.clear();
        list.add(topic);
        earliest = d;
      }
    }
    if (list.isEmpty()) earliest = Duration.MAX_VALUE;
    return Pair.of(list, earliest);

  }

  private ExecutionState<?> getTaskExecutionState(TaskId taskId) {
    var execState = tasks.get(taskId);
    if (execState == null && oldEngine != null) {
      execState = oldEngine.getTaskExecutionState(taskId);
    }
    return execState;
  }

  /**
   * If task is not already stale, record the task's staleness at specified time in this.staleTasks,
   * remove task reads and effects from the timeline and cell read history, and then create the task
   * and schedule a job for it.
   * @param taskId id of the task being set stale
   * @param time time when the task becomes stale
   */
  public void setTaskStale(TaskId taskId, Duration time) {
    var staleTime = staleTasks.get(taskId);
    if (staleTime != null) {
      if (staleTime.noLongerThan(time)) {
        // already marked stale by this time; a stale task cannot become unstale because we can't see it's internal state
        String taskName = getNameForTask(taskId);
        System.err.println("WARNING: trying to set stale task stale at earlier time; this should not be possible; cannot re-execute a task more than once: TaskId = " + taskId + ", task name = \"" + taskName + "\"");
      }
      return;
    }
    // find parent task to execute and mark parents stale
    TaskId parentId = taskId;
    while (parentId != null) {
      staleTasks.put(taskId, time);
      // if we cache task lambdas/TaskFactorys, we want to stop at the first existing lambda/TakFactory
      if (oldEngine.getFactoryForTaskId(parentId) != null) {
        break;
      }
      if (oldEngine.isActivity(parentId)) {
        break;
      }
      if (oldEngine.isDaemonTask(parentId)) {
        break;
      }
      var nextParentId = oldEngine.getTaskParent(taskId);
      if (nextParentId == null) break;
      parentId = nextParentId;
    }

    final ExecutionState<?> execState = oldEngine.getTaskExecutionState(parentId);
    final Duration taskStart;
    if (execState != null) taskStart = execState.startOffset(); // WARNING: assumes offset is from same plan start
    else {
      //taskStart = Duration.ZERO;
      throw new RuntimeException("Can't find task start!");
    }
    rescheduleTask(parentId, taskStart);
    removeTaskHistory(parentId, time);
  }

  /**
   * For the next time t that a set of tasks could potentially have a stale read, check if any read is stale for
   * each of those tasks, and, if so, mark them stale at t and schedule them to re-run.
   * <p>
   * This method assumes that these are reads that occurred in the previous simulation and thus have an EventGraph
   * in the old SimulationEngine's timeline with the read noop.  If the current timeline has an EventGraph at this
   * same time, it is assumed to also have the noop events.
   *
   * @param earliestStaleReads the time of the potential stale reads along with the tasks and the potentially stale topics they read
   */
  public void rescheduleStaleTasks(Pair<Duration, Map<TaskId, HashSet<Pair<Topic<?>, Event>>>> earliestStaleReads) {
    // Test to see if read value has changed.  If so, reschedule the affected task
    Duration timeOfStaleReads = earliestStaleReads.getLeft();
    for (Map.Entry<TaskId, HashSet<Pair<Topic<?>, Event>>> entry : earliestStaleReads.getRight().entrySet()) {
      final var taskId = entry.getKey();
      for (Pair<Topic<?>, Event> pair : entry.getValue()) {
        final var topic = pair.getLeft();
        final var noop = pair.getRight();
        // Need to step cell up to the point of the read
        // First, step up the cell to the time before the event graph where the read takes place and then
        // make a duplicate of the cell since partial evaluation of an event graph makes the cell unusable
        // for stepping further.
        var steppedCell = timeline.getCell(topic, timeOfStaleReads, false);
        final Cell<?> tempCell = steppedCell.duplicate();
        List<TemporalEventSource.TimePoint.Commit> events = this.timeline.getCombinedCommitsByTime().get(timeOfStaleReads);
        if (events == null || events.isEmpty()) throw new RuntimeException("No EventGraph for potentially stale read.");
        this.timeline.stepUp(tempCell, events.get(events.size()-1).events(), noop, false);
        // Assumes that the same noop event for the read exists at the same time in the oldTemporalEventSource.
        var oldEvents = this.timeline.oldTemporalEventSource.getCombinedCommitsByTime().get(timeOfStaleReads);
        if (oldEvents == null || oldEvents.isEmpty()) throw new RuntimeException("No old EventGraph for potentially stale read.");
        if (timeline.isTopicStale(topic, timeOfStaleReads) || !oldEvents.equals(events)) {
          // Assumes the old cell has been stepped up to the same time already.  TODO: But, if not stale, shouldn't the old cell not exist or not be stepped up, in which case we duplicate to get the old cell instead unless the old event graph is the same?
          var tempOldCell = timeline.getOldCell(steppedCell).map(Cell::duplicate);
          this.timeline.oldTemporalEventSource.stepUp(tempOldCell.orElseThrow(),
                                                      oldEvents.get(oldEvents.size()-1).events(), noop, false);
          if (!tempCell.getState().equals(tempOldCell.get().getState())) {
            if (debug) System.out.println("Stale read: new cell state (" + tempCell.getState() + ") != od cell state (" + tempOldCell.get().getState() + ")");
            // Mark stale and reschedule task
            setTaskStale(taskId, timeOfStaleReads);
            break;  // rescheduled task, so can move on to the next task
          }
        }
      }  // for Pair<Topic<?>, Event>
    }  // for Map.Entry<TaskId, HashSet<Pair<Topic<?>, Event>>>
  }

  public TaskId getTaskIdForDirectiveId(ActivityDirectiveId id) {
    var taskId = this.taskInfo.getTaskIdForDirectiveId(id);
    if (taskId == null && oldEngine != null) {
      taskId = oldEngine.getTaskIdForDirectiveId(id);
    }
    return taskId;
  }

  private TaskId getTaskIdForFactory(TaskFactory<?> taskFactory) {
    var taskId = taskIdsForFactories.get(taskFactory);
    if (taskId == null && oldEngine != null) {
      taskId = oldEngine.getTaskIdForFactory(taskFactory);
    }
    return taskId;
  }

  private TaskFactory<?> getFactoryForTaskId(TaskId taskId) {
    var taskFactory = taskFactories.get(taskId);
    if (taskFactory == null && oldEngine != null) {
      taskFactory = oldEngine.getFactoryForTaskId(taskId);
    }
    return taskFactory;
  }

  private TreeMap<Duration, List<EventGraph<Event>>> getCombinedEventsByTask(TaskId taskId) {
    var newEvents = this.timeline.eventsByTask.get(taskId);
    if (oldEngine == null) return newEvents;
    SimulationEngine engine = this;
    TreeMap<Duration, List<EventGraph<Event>>> oldEvents = null;
    while (oldEvents == null && engine != null) {
      oldEvents = engine._oldEventsByTask.get(taskId);
      engine = engine.oldEngine;
    }
    if (oldEvents != null) {
      this._oldEventsByTask.put(taskId, oldEvents);
    }
    return TemporalEventSource.mergeMapsFirstWins(newEvents, oldEvents);
  }
  private HashMap<TaskId, TreeMap<Duration, List<EventGraph<Event>>>> _oldEventsByTask = new HashMap<>();


  // TODO -- make recursive calls here non-recursive (like in getCombinedEventsByTask()),
  // TODO -- including getSimulatedActivityIdForTaskId(), setCurTime(), and CombinedSimulationResults

  //private HashSet<TaskId> _missingOldSimulatedActivityIds = new HashSet<>(); // short circuit deeply nested searches for taskIds that have
  private SimulatedActivityId getSimulatedActivityIdForTaskId(TaskId taskId) {
    //if (_missingOldSimulatedActivityIds.contains(taskId)) return
    var simId = taskToSimulatedActivityId == null ? null : taskToSimulatedActivityId.get(taskId.id());
    if (simId == null && oldEngine != null) {
      // If this activity hasn't been seen in this simulation, it may be in a past one; this check avoids unnecessarily recursing
      if (!this.isActivity(taskId)) {
        simId = oldEngine.getSimulatedActivityIdForTaskId(taskId);
      }
    }
    return simId;
  }

  public void removeActivity(final TaskId taskId) {
    var simId = getSimulatedActivityIdForTaskId(taskId);
    removedActivities.add(simId);
    removeTaskHistory(taskId, Duration.MIN_VALUE);
  }

  public void removeTaskHistory(final TaskId taskId, Duration startingAfterTime) { // TODO -- need graph index with time
    // Look for the task's Events in the old and new timelines.
    if (debug) System.out.println("removeTaskHistory(taskId=" + taskId + " : " + getNameForTask(taskId) + ", startingAfterTime=" + startingAfterTime + ") BEGIN");
    final TreeMap<Duration, List<EventGraph<Event>>> graphsForTask = this.timeline.eventsByTask.get(taskId);
    final TreeMap<Duration, List<EventGraph<Event>>> oldGraphsForTask = this.oldEngine.getCombinedEventsByTask(taskId);
    if (debug) System.out.println("old combined graphs = " + oldGraphsForTask);
    if (debug) System.out.println("new local graphs = " + graphsForTask);
    if (debug) {
      final TreeMap<Duration, List<EventGraph<Event>>> combinedGraphsForTask = this.getCombinedEventsByTask(taskId);
      if (debug) System.out.println("new combined graphs = " + graphsForTask);
    }
    var allKeys = new TreeSet<Duration>();
    if (graphsForTask != null) {
      allKeys.addAll(graphsForTask.keySet());
    }
    if (oldGraphsForTask != null) {
      allKeys.addAll(oldGraphsForTask.keySet());
    }
    for (Duration time : allKeys) {
      if (time.noLongerThan(startingAfterTime)) continue;
      List<EventGraph<Event>> gl = graphsForTask == null ? null : graphsForTask.get(time); // If old graph is already replaced used the replacement
      if (gl == null || gl.isEmpty()) gl = oldGraphsForTask == null ? null : oldGraphsForTask.get(time);  // else we can replace the old graph
      for (var g : gl) {
//        // invalidate topics for cells affected by the task in the old graph so that resource values are checked at
//        // this time to erase effects on resources  -- TODO: this doesn't work!  only one scheduled job per resource
        var s = new HashSet<Topic<?>>();
        TemporalEventSource.extractTopics(s, g, e -> taskId.equals(e.provenance()));
        //s.forEach(topic -> invalidateTopic(topic, time));
        s.forEach(topic -> timeline.setTopicStale(topic, time));
        // replace the old graph with one without the task's events, updating data structures
        var newG = g.filter(e -> !taskId.equals(e.provenance()));
        if (newG != g) {
          if (debug) System.out.println("replacing old graph=" + g + " with new graph=" + newG + " at time " + time);
          timeline.replaceEventGraph(g, newG);
          updateTaskInfo(newG);
          removedCellReadHistory.computeIfAbsent(time, $ -> new HashSet<>()).add(taskId);
        }
      }
    }
    // remove task from taskInfo data structures
    taskInfo.removeTask(taskId);

    // Remove children, too!
    var children = this.oldEngine.getTaskChildren(taskId);
    if (children != null) children.forEach(c -> removeTaskHistory(c, startingAfterTime));
    if (debug) {
      final TreeMap<Duration, List<EventGraph<Event>>> localGraphsForTask = this.timeline.eventsByTask.get(taskId);
      final TreeMap<Duration, List<EventGraph<Event>>> combinedGraphsForTask = this.getCombinedEventsByTask(taskId);
      System.out.println("resulting local graphs = " + localGraphsForTask);
      System.out.println("resulting combined graphs = " + combinedGraphsForTask);
    }
    if (debug) System.out.println("removeTaskHistory(taskId=" + taskId + " : " + getNameForTask(taskId) + ", startingAfterTime=" + startingAfterTime +  ") END");
  }

  private static ExecutorService getLoomOrFallback() {
    // Try to use Loom's lightweight virtual threads, if possible. Otherwise, just use a thread pool.
    // This approach is inspired by that of Javalin 5.
    // https://github.com/javalin/javalin/blob/97e9e23ebe8f57aa353bc7a45feb560ad61e50a0/javalin/src/main/java/io/javalin/util/ConcurrencyUtil.kt#L48-L51
    try {
      // Use reflection to avoid needing `--enable-preview` at compile-time.
      // If the runtime JVM is run with `--enable-preview`, this should succeed.
      return (ExecutorService) Executors.class.getMethod("newVirtualThreadPerTaskExecutor").invoke(null);
    } catch (final ReflectiveOperationException ex) {
      return Executors.newCachedThreadPool($ -> {
        final var t = new Thread($);
        // TODO: Make threads non-daemons.
        //  We're marking these as daemons right now solely to ensure that the JVM shuts down cleanly in lieu of
        //  proper model lifecycle management.
        //  In fact, daemon threads can mask bad memory leaks: a hanging thread is almost indistinguishable
        //  from a dead thread.
        t.setDaemon(true);
        return t;
      });
    }
  }

  /** Schedule a new task to be performed at the given time. */
  public <Return> TaskId scheduleTask(final Duration startTime, final TaskFactory<Return> state, TaskId taskIdToUse) {
    if (startTime.isNegative()) throw new IllegalArgumentException("Cannot schedule a task before the start time of the simulation");

    final var task = taskIdToUse == null ? TaskId.generate() : taskIdToUse;
    this.tasks.put(task, new ExecutionState.InProgress<>(startTime, state.create(this.executor)));
    this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(startTime));
    return task;
  }

  /**
   * Has this resource already been simulated?
   * @param name the name of the resource used for lookup
   * @return whether the resource already has segments recorded, indicating that it has at least been partly simulated
   */
  public boolean hasSimulatedResource(final String name) {
    final var id = new ResourceId(name);
    final ProfilingState<?> state = this.resources.get(id);
    if (state == null) {
      return false;
    }
    final Profile<?> profile = state.profile();
    return profile != null && profile.segments().size() > 0;
  }

  /**
   * Register (if not already registered) a resource whose profile should be accumulated over time.
   * Schedule a job to get resource values starting at the time specified.
   */
  public <Dynamics>
  void trackResource(final String name, final Resource<Dynamics> resource, final Duration nextQueryTime) {
    final var id = new ResourceId(name);
    final ProfilingState<?> state = this.resources.get(id);
    if (state == null) {
      this.resources.put(id, ProfilingState.create(resource));
    } else {
      // TODO -- should we do some kind of reset, like clearing segments after nextQueryTime?
    }
    this.scheduledJobs.schedule(JobId.forResource(id), SubInstant.Resources.at(nextQueryTime));
  }

  public boolean isTaskStale(TaskId taskId, Duration timeOffset) {
    final Duration staleTime = this.staleTasks.get(taskId);
    if (staleTime == null) {
      return true;  // This is only asked of scheduled tasks, so if there is no stale time,
                    // then the task must be new or modified by the user, so it should always be considered stale.
      // NOTE: In the case of a modified task, is it possible to predict that it will have no effect?
      // NOTE: No, even if only the start time changed, effects could depend on the start time.  A new interface would
      // NOTE: be needed to convey how to determine staleness.
    }
    return staleTime.noLongerThan(timeOffset);
  }

  /** Schedules any conditions or resources dependent on the given topic to be re-checked at the given time. */
  public void invalidateTopic(final Topic<?> topic, final Duration invalidationTime) {
    if (debug) System.out.println("invalidateTopic(" + topic + ", " + invalidationTime + ")");
    final var resources = this.waitingResources.invalidateTopic(topic);
    if (debug && !resources.isEmpty()) {
      if (debug) System.out.println("SimulationEngine.invalidateTopic(): " + topic + " at " + invalidationTime + " and schedule jobs for " + resources.stream().map(r -> r.id()).toList());
    }
    for (final var resource : resources) {
      this.scheduledJobs.schedule(JobId.forResource(resource), SubInstant.Resources.at(invalidationTime));
    }

    final var conditions = this.waitingConditions.invalidateTopic(topic);
    if (trace) System.out.println("invalidateTopic(): conditions waiting on topic: " + conditions);
    for (final var condition : conditions) {
      // If we were going to signal tasks on this condition, well, don't do that.
      // Schedule the condition to be rechecked ASAP.
      this.scheduledJobs.unschedule(JobId.forSignal(SignalId.forCondition(condition)));
      var cjid = JobId.forCondition(condition);
      var t = SubInstant.Conditions.at(invalidationTime);
      if (trace) System.out.println("invalidateTopic(): schedule(ConditionJobId " + cjid + " at time " + t + ")");
      this.scheduledJobs.schedule(cjid, t);
    }
  }

  /** Returns the offset time of the next batch of scheduled jobs. */
  public Duration timeOfNextJobs() {
    return this.scheduledJobs.timeOfNextJobs();
  }

  /** Performs a collection of tasks concurrently, extending the given timeline by their stateful effects. */
  public void step(final Duration maximumTime, final Topic<Topic<?>> queryTopic,
                   final Consumer<Duration> simulationExtentConsumer) {
    if (debug) System.out.println("step(): begin -- time = " + curTime() + ", step " + stepIndexAtTime);
    var timeOfNextJobs = timeOfNextJobs();
    var nextTime = timeOfNextJobs;

    Pair<Duration, Map<TaskId, HashSet<Pair<Topic<?>, Event>>>> earliestStaleReads = null;
    Duration staleReadTime = null;
    Pair<List<Topic<?>>, Duration> earliestStaleTopics = null;
    Pair<List<Topic<?>>, Duration> earliestStaleTopicOldEvents = null;
    Duration staleTopicTime = Duration.MAX_VALUE;
    Duration staleTopicOldEventTime = Duration.MAX_VALUE;
    Duration conditionTime = Duration.MAX_VALUE;
    Pair<List<Topic<?>>, Duration> earliestConditionTopics = null;

    if (oldEngine != null && nextTime.noShorterThan(curTime())) {
      if (resourceTracker == null) {
        earliestStaleTopics = earliestStaleTopics(curTime(), nextTime);  // might want to not limit by nextTime and cache for future iterations
        //if (debug) System.out.println("earliestStaleTopics(" + curTime() + ", " + Duration.min(nextTime, maximumTime) + ") = " + earliestStaleTopics);
        staleTopicTime = earliestStaleTopics.getRight();
        nextTime = Duration.min(nextTime, staleTopicTime);

        earliestStaleTopicOldEvents = nextStaleTopicOldEvents(curTime(), Duration.min(nextTime, maximumTime));
        //if (debug) System.out.println("nextStaleTopicOldEvents(" + curTime() + ", " + Duration.min(nextTime, maximumTime) + ") = " + earliestStaleTopicOldEvents);
        staleTopicOldEventTime = earliestStaleTopicOldEvents.getRight();
        nextTime = Duration.min(nextTime, staleTopicOldEventTime);
      }

      earliestStaleReads = earliestStaleReads(
          curTime(),
          nextTime);  // might want to not limit by nextTime and cache for future iterations
      staleReadTime = earliestStaleReads.getLeft();
      nextTime = Duration.min(nextTime, staleReadTime);

      earliestConditionTopics = earliestConditionTopics(curTime(), nextTime);
      conditionTime = earliestConditionTopics.getRight();
      nextTime = Duration.min(nextTime, conditionTime);
    }

    // Increment real time, if necessary.
    var timeForDelta = Duration.min(nextTime, maximumTime);
    final var delta = timeForDelta.minus(Duration.max(curTime(), Duration.ZERO));
    setCurTime(timeForDelta);
    if (!delta.isZero()) {
      stepIndexAtTime = 0;
      overlayingEvents = false;
    }
    // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
    //   even if they occur at the same real time.

    Set<Topic<?>> invalidatedTopics = new HashSet<>();

    if (oldEngine != null) {

      if (nextTime.longerThan(maximumTime) || nextTime.isEqualTo(Duration.MAX_VALUE)) {
        if (debug) System.out.println("step(): end -- time elapsed ("
                                      + curTime()
                                      + ") past maximum ("
                                      + maximumTime
                                      + ")");
        return;
      }

      if (resourceTracker == null && staleTopicTime.isEqualTo(nextTime)) {
        if (debug) System.out.println("earliestStaleTopics at " + nextTime + " = " + earliestStaleTopics);
        for (Topic<?> topic : earliestStaleTopics.getLeft()) {
          invalidateTopic(topic, nextTime);
          invalidatedTopics.add(topic);
        }
      }

      if (resourceTracker == null && staleTopicOldEventTime.isEqualTo(nextTime)) {
        if (debug) System.out.println("nextStaleTopicOldEvents at " + nextTime + " = " + earliestStaleTopicOldEvents);
        for (Topic<?> topic : earliestStaleTopicOldEvents
            .getLeft()
            .stream()
            .filter(t -> !invalidatedTopics.contains(t))
            .toList()) {
          invalidateTopic(topic, nextTime);
          invalidatedTopics.add(topic);
        }
      }

      if (conditionTime.isEqualTo(nextTime)) {
        if (debug) System.out.println("earliestConditionTopics at " + nextTime + " = " + earliestConditionTopics);
        for (Topic<?> topic : earliestConditionTopics
            .getLeft()
            .stream()
            .filter(t -> !invalidatedTopics.contains(t))
            .toList()) {
          invalidateTopic(topic, nextTime);
          invalidatedTopics.add(topic);
        }
      }
    }
    if (staleReadTime != null && staleReadTime.isEqualTo(nextTime)) {
      if (debug) System.out.println("earliestStaleReads at " + nextTime + " = " + earliestStaleReads);
      rescheduleStaleTasks(earliestStaleReads);
    } else
    if (timeOfNextJobs.isEqualTo(nextTime) && invalidatedTopics.isEmpty()) {

      final var batch = this.scheduledJobs.extractNextJobs(maximumTime);
      // If we're signaling based on a condition, we need to untrack the condition before any tasks run.
      // Otherwise, we could see a race if one of the tasks running at this time invalidates state
      // that the condition depends on, in which case we might accidentally schedule an update for a condition
      // that no longer exists.
      for (final var job : batch.jobs()) {
        if (!(job instanceof JobId.SignalJobId j)) continue;
        if (!(j.id() instanceof SignalId.ConditionSignalId s)) continue;

        this.conditions.remove(s.id());
        this.waitingConditions.unsubscribeQuery(s.id());
      }

      //setCurTime(batch.offsetFromStart());
      var tip = EventGraph.<Event>empty();
      for (final var job$ : batch.jobs()) {
        tip = EventGraph.concurrently(tip, TaskFrame.run(job$, this.cells, (job, frame) -> {
          this.performJob(job, frame, curTime(), maximumTime, queryTopic);
        }));
      }

      this.timeline.add(tip, curTime(), stepIndexAtTime);
      updateTaskInfo(tip);
      stepIndexAtTime += 1;
    }
    if (debug) System.out.println("step(): end -- time = " + curTime() + ", step " + stepIndexAtTime);
  }

  /** Performs a single job. */
  private void performJob(
      final JobId job,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final Duration maximumTime,
      final Topic<Topic<?>> queryTopic) {
    if (job instanceof JobId.TaskJobId j) {
      this.stepTask(j.id(), frame, currentTime, queryTopic);
    } else if (job instanceof JobId.SignalJobId j) {
      this.stepSignalledTasks(j.id(), frame);
    } else if (job instanceof JobId.ConditionJobId j) {
      this.updateCondition(j.id(), frame, currentTime, maximumTime, queryTopic);
    } else if (job instanceof JobId.ResourceJobId j) {
      assert resourceTracker == null;
      this.updateResource(j.id(), frame, currentTime);
    } else {
      throw new IllegalArgumentException("Unexpected subtype of %s: %s".formatted(JobId.class, job.getClass()));
    }
  }

  /** Perform the next step of a modeled task. */
  public void stepTask(final TaskId task, final TaskFrame<JobId> frame, final Duration currentTime,
                       final Topic<Topic<?>> queryTopic) {
    // The handler for each individual task stage is responsible
    //   for putting an updated lifecycle back into the task set.
    var lifecycle = this.tasks.remove(task);

    stepTaskHelper(task, frame, currentTime, lifecycle, queryTopic);
  }

  private <Return> void stepTaskHelper(
      final TaskId task,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final ExecutionState<Return> lifecycle, final Topic<Topic<?>> queryTopic)
  {
    // Extract the current modeling state.
    if (lifecycle instanceof ExecutionState.InProgress<Return> e) {
      stepEffectModel(task, e, frame, currentTime, queryTopic);
    } else if (lifecycle instanceof ExecutionState.AwaitingChildren<Return> e) {
      stepWaitingTask(task, e, frame, currentTime);
    } else {
      // TODO: Log this issue to somewhere more general than stderr.
      System.err.println("Task %s is ready but in unexpected execution state %s".formatted(task, lifecycle));
    }
  }

  /** Make progress in a task by stepping its associated effect model forward. */
  private <Return> void stepEffectModel(
      final TaskId task,
      final ExecutionState.InProgress<Return> progress,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final Topic<Topic<?>> queryTopic) {
    // Step the modeling state forward.
    final var scheduler = new EngineScheduler(currentTime, task, frame, queryTopic);
    final var status = progress.state().step(scheduler);

    // TODO: Report which topics this activity wrote to at this point in time. This is useful insight for any user.
    // TODO: Report which cells this activity read from at this point in time. This is useful insight for any user.

    // Based on the task's return status, update its execution state and schedule its resumption.
    if (status instanceof TaskStatus.Completed<Return>) {
      final var children = new LinkedList<>(this.taskChildren.getOrDefault(task, Collections.emptySet()));

      this.tasks.put(task, progress.completedAt(currentTime, children));
      this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime));
    } else if (status instanceof TaskStatus.Delayed<Return> s) {
      if (s.delay().isNegative()) throw new IllegalArgumentException("Cannot schedule a task in the past");

      this.tasks.put(task, progress.continueWith(s.continuation()));
      this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime.plus(s.delay())));
    } else if (status instanceof TaskStatus.CallingTask<Return> s) {
      final var target = TaskId.generate();
      SimulationEngine.this.tasks.put(target, new ExecutionState.InProgress<>(currentTime, s.child().create(this.executor)));
      SimulationEngine.this.taskParent.put(target, task);
      SimulationEngine.this.taskChildren.computeIfAbsent(task, $ -> new HashSet<>()).add(target);
      frame.signal(JobId.forTask(target));

      this.tasks.put(task, progress.continueWith(s.continuation()));
      this.waitingTasks.subscribeQuery(task, Set.of(SignalId.forTask(target)));
    } else if (status instanceof TaskStatus.AwaitingCondition<Return> s) {
      final var condition = ConditionId.generate(task);
      this.conditions.put(condition, s.condition());
      var jid = JobId.forCondition(condition);
      var t = SubInstant.Conditions.at(currentTime);
      if (trace) System.out.println("stepEffectModel(TaskId=" + task + "): conditionId = " + condition + ", AwaitingCondition s = " + s + ", ConditionJobId = " + jid + ", at time " + t);
      this.scheduledJobs.schedule(jid, t);

      this.tasks.put(task, progress.continueWith(s.continuation()));
      this.waitingTasks.subscribeQuery(task, Set.of(SignalId.forCondition(condition)));
    } else {
      throw new IllegalArgumentException("Unknown subclass of %s: %s".formatted(TaskStatus.class, status));
    }
  }

  /** Make progress in a task by checking if all of the tasks it's waiting on have completed. */
  private <Return> void stepWaitingTask(
      final TaskId task,
      final ExecutionState.AwaitingChildren<Return> awaiting,
      final TaskFrame<JobId> frame,
      final Duration currentTime
  ) {
    // TERMINATION: We break when there are no remaining children,
    //   and we always remove one if we don't break for other reasons.
    while (true) {
      if (awaiting.remainingChildren().isEmpty()) {
        this.tasks.put(task, awaiting.joinedAt(currentTime));
        frame.signal(JobId.forSignal(SignalId.forTask(task)));
        break;
      }

      final var nextChild = awaiting.remainingChildren().getFirst();
      if (!(this.tasks.get(nextChild) instanceof ExecutionState.Terminated<?>)) {
        this.tasks.put(task, awaiting);
        this.waitingTasks.subscribeQuery(task, Set.of(SignalId.forTask(nextChild)));
        break;
      }

      // This child is complete, so skip checking it next time; move to the next one.
      awaiting.remainingChildren().removeFirst();
    }
  }

  /** Cause any tasks waiting on the given signal to be resumed concurrently with other jobs in the current frame. */
  public void stepSignalledTasks(final SignalId signal, final TaskFrame<JobId> frame) {
    final var tasks = this.waitingTasks.invalidateTopic(signal);
    for (final var task : tasks) frame.signal(JobId.forTask(task));
  }

  /** Determine when a condition is next true, and schedule a signal to be raised at that time. */
  public void updateCondition(
      final ConditionId condition,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final Duration horizonTime,
      final Topic<Topic<?>> queryTopic) {
    if (trace) System.out.println("updateCondition(ConditionId=" + condition + ", queryTopic=" + queryTopic + ")");
    final var querier = new EngineQuerier(currentTime, frame, queryTopic, condition.sourceTask());
    final var prediction = this.conditions
        .get(condition)
        .nextSatisfied(querier, Duration.MAX_VALUE) //horizonTime.minus(currentTime)
        .map(currentTime::plus);

    if (trace) System.out.println("updateCondition(): waitingConditions.subscribeQuery(conditionId=" + condition + ", querier.referencedTopics=" + querier.referencedTopics + ")");
    this.waitingConditions.subscribeQuery(condition, querier.referencedTopics);

    final Optional<Duration> expiry = querier.expiry.map(d -> currentTime.plus((Duration)d));
    if (trace) System.out.println("updateCondition(): expiry = " + expiry);
    if (prediction.isPresent() && (expiry.isEmpty() || prediction.get().shorterThan(expiry.get()))) {
      var csid = SignalId.forCondition(condition);
      var sjid = JobId.forSignal(csid);
      var t = SubInstant.Tasks.at(prediction.get());
      if (trace) System.out.println("updateCondition(): schedule(SignalJobId " + sjid + " for ConditionSignalID " + csid + " + at time " + t + ")");
      this.scheduledJobs.schedule(sjid, t);
    } else {
      // Try checking again later -- where "later" is in some non-zero amount of time!
      final var nextCheckTime = Duration.max(expiry.orElse(Duration.MAX_VALUE), currentTime.plus(Duration.EPSILON));
      var cjid = JobId.forCondition(condition);
      var t = SubInstant.Conditions.at(nextCheckTime);
      if (trace) System.out.println("updateCondition(): schedule(ConditionJobId " + cjid + " at time " + t + ")");
      this.scheduledJobs.schedule(cjid, t);
    }
  }



  /** Get the current behavior of a given resource and accumulate it into the resource's profile. */
  public void updateResource(
      final ResourceId resource,
      final TaskFrame<JobId> frame,
      final Duration currentTime
  ) {
    // TODO -- this would be better with the ResourceTracker from the branch, prototype/excise-resources-from-sim-engine
    if (debug) System.out.println("SimulationEngine.updateResource(" + resource + ", " + currentTime + ")");
    // We want to avoid saving profile segments if they aren't changing.  We also don't want to compute the resource if
    // none of the cells on which it depends are stale.
    assert resourceTracker == null;
    boolean skipResourceEvaluation = false;
    Set<Topic<?>> referencedTopics = null;
    if (oldEngine != null) {
      var ebt = oldEngine.timeline.getCombinedCommitsByTime();
      var latestTime = ebt.floorKey(currentTime);
      // Don't skip at the start of simulation.  We need the initial topics to know when stale.
      // TODO: REVIEW: Actually, we could derive the initial topics from the events in the old timeline.  Should we?
      if (currentTime.isEqualTo(Duration.ZERO)) {  // Duration.ZERO is assumed to be simulationStartTime
        skipResourceEvaluation = false;
      }
      // If no events since plan start, then can't be stale, so nothing to do.
      else if (latestTime == null) skipResourceEvaluation = true;
      else {
        // Note that there may or may not be events at this currentTime.
        // So, how can we know the resource is not stale?
        // - No cells are stale
        // - If the past resource value was not based on stale information and matched the previous simulation
        //   (henceforth, the resource is not stale), and if the resource's referencedTopics in waitingResources
        //   are not stale, hen the evaluation may be skipped.
        // - So, should we choose a different expiry? Probably not--just make this evaluation fast.
        //   And, with staleness, we can determine that we need not invalidate a topic in some cases.

        // Check if any of the resource's referenced topics are stale
        referencedTopics = this.referencedTopics.get(resource); //this.waitingResources.getTopics(resource);
        if (debug) System.out.println("topics for resource " + resource.id() + " at " + currentTime + ": " + referencedTopics);
        var resourceIsStale = referencedTopics.stream().anyMatch(t -> timeline.isTopicStale(t, currentTime));
        if (debug) System.out.println("topic is stale for " + resource.id() + " at " + currentTime + ": " +
                                      referencedTopics.stream().map(t -> "" + t + "=" +
                                                                         timeline.isTopicStale(t, currentTime)).toList());
        if (debug) System.out.println("timeline.staleTopics: " + timeline.staleTopics);
        if (!resourceIsStale) {
          if (debug) System.out.println("skipping evaluation of resource " + resource.id() + " at " + currentTime);
          skipResourceEvaluation = true;
        } else {
          // Check for the case where the effect is removed.  If the timeline has events at this time, but they do not
          // include any of this resource's referenced topics, then the events were removed, and we need not generate
          // a profile segment for the resource (setting skipResourceEvaluation = true).
          skipResourceEvaluation = false;
          final List<TemporalEventSource.TimePoint.Commit> commits = timeline.commitsByTime.get(currentTime);
          var topicsRemoved = timeline.topicsOfRemovedEvents.get(currentTime);
          skipResourceEvaluation =
              topicsRemoved != null &&
              referencedTopics.stream().allMatch(t -> !timeline.isTopicStale(t, currentTime) ||
                                            (!commits.stream().anyMatch(c -> c.topics().contains(t)) &&  // assumes replaced EventGraphs in current timeline
                                             topicsRemoved.contains(t)));
          if (skipResourceEvaluation) {
            this.timeline.removedResourceSegments.computeIfAbsent(currentTime, $ -> new HashSet<>()).add(resource.id());
          }
          if (debug) System.out.println("check for removed effects for resource " + resource.id() + " at " + currentTime + "; skipResourceEvaluation = " + skipResourceEvaluation);
        }

      }
    }

    final var querier = new EngineQuerier(currentTime, frame);
    if (!skipResourceEvaluation) {
      var profiles = this.resources.get(resource);
      // TODO: Should we check if the profile state hasn't been changing and if so not record them?
      //       if (profileIsChanging)
      {
        profiles.append(currentTime, querier);
        if (debug) System.out.println("resource " + resource.id() + " updated profile: " + profiles);
        referencedTopics = querier.referencedTopics;
      }
    }

    // Even if we aren't going to update the resource profile, we need to at least re-subscribe to the old cell topics
    if (referencedTopics != null && !referencedTopics.isEmpty()) {
      this.waitingResources.subscribeQuery(resource, referencedTopics);
      this.referencedTopics.put(resource, referencedTopics);
      if (debug) System.out.println("querier, " + querier + " subscribing " + resource.id() + " to referenced topics: " + querier.referencedTopics);
    }

    final Optional<Duration> expiry = querier.expiry.map(d -> currentTime.plus((Duration)d));
    if (expiry.isPresent()) {
      this.scheduledJobs.schedule(JobId.forResource(resource), SubInstant.Resources.at(expiry.get()));
    }
  }

  /** Resets all tasks (freeing any held resources). The engine should not be used after being closed. */
  @Override
  public void close() {
    for (final var task : this.tasks.values()) {
      if (task instanceof ExecutionState.InProgress r) {
        r.state.release();
      }
    }

    this.executor.shutdownNow();
  }

  /** Determine if a given task has fully completed. */
  public boolean isTaskComplete(final TaskId task) {
    return (this.tasks.get(task) instanceof ExecutionState.Terminated);
  }

  public MissionModel<?> getMissionModel() {
    return this.missionModel;
  }

  public Duration curTime() {
    if (timeline == null) {
      return Duration.ZERO;
    }
    return timeline.curTime();
  }

  public void setCurTime(Duration time) {
    this.timeline.setCurTime(time);
    if (this.oldEngine != null) {
      this.oldEngine.setCurTime(time);
    }
  }

  public Map<String, Map<ActivityDirectiveId, ActivityDirective>> diffDirectives(Map<ActivityDirectiveId, ActivityDirective> newDirectives) {
    Map<String, Map<ActivityDirectiveId, ActivityDirective>> diff = new HashMap<>();
    final var oldDirectives = scheduledDirectives;
    diff.put("added", newDirectives.entrySet().stream().filter(e -> !oldDirectives.containsKey(e.getKey())).collect(
        Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
    diff.put("removed", oldDirectives.entrySet().stream().filter(e -> !newDirectives.containsKey(e.getKey())).collect(
        Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
    diff.put("modified", newDirectives.entrySet().stream().filter(e -> oldDirectives.containsKey(e.getKey()) && !e.getValue().equals(oldDirectives.get(e.getKey()))).collect(
        Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
    return diff;
  }

  public boolean hasJobsScheduledThrough(final Duration givenTime) {
    return this.scheduledJobs
        .min()
        .map($ -> $.project().noLongerThan(givenTime))
        .orElse(false);
  }

  public record TaskInfo(
      Map<String, ActivityDirectiveId> taskToPlannedDirective,
      Map<ActivityDirectiveId, TaskId> directiveIdToTaskId,
      Map<String, SerializedActivity> input,
      Map<String, SerializedValue> output
  ) {
    public TaskInfo() {
      this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public boolean isActivity(final TaskId id) {
      return this.input.containsKey(id.id());
    }

    public TaskId getTaskIdForDirectiveId(ActivityDirectiveId id) {
      return directiveIdToTaskId.get(id);
    }

    public void removeTask(final TaskId id) {
      var directiveId = taskToPlannedDirective.remove(id.id());
      if (directiveId != null) directiveIdToTaskId.remove(directiveId);
      input.remove(id.id());
      output.remove(id.id());
    }

    public record Trait(Iterable<SerializableTopic<?>> topics, Topic<ActivityDirectiveId> activityTopic) implements EffectTrait<Consumer<TaskInfo>> {
      @Override
      public Consumer<TaskInfo> empty() {
        return taskInfo -> {};
      }

      @Override
      public Consumer<TaskInfo> sequentially(final Consumer<TaskInfo> prefix, final Consumer<TaskInfo> suffix) {
        return taskInfo -> { prefix.accept(taskInfo); suffix.accept(taskInfo); };
      }

      @Override
      public Consumer<TaskInfo> concurrently(final Consumer<TaskInfo> left, final Consumer<TaskInfo> right) {
        // SAFETY: For each task, `left` commutes with `right`, because no task runs concurrently with itself.
        return taskInfo -> { left.accept(taskInfo); right.accept(taskInfo); };
      }

      public Consumer<TaskInfo> atom(final Event ev) {
        return taskInfo -> {
          // Identify activities.
          ev.extract(this.activityTopic)
            .ifPresent(directiveId -> {
              taskInfo.taskToPlannedDirective.put(ev.provenance().id(), directiveId);
              taskInfo.directiveIdToTaskId.put(directiveId, ev.provenance());
            });

          for (final var topic : this.topics) {
            // Identify activity inputs.
            extractInput(topic, ev, taskInfo);

            // Identify activity outputs.
            extractOutput(topic, ev, taskInfo);
          }
        };
      }

      private static <T>
      void extractInput(final SerializableTopic<T> topic, final Event ev, final TaskInfo taskInfo) {
        if (!topic.name().startsWith("ActivityType.Input.")) return;

        ev.extract(topic.topic()).ifPresent(input -> {
          final var activityType = topic.name().substring("ActivityType.Input.".length());

          taskInfo.input.put(
              ev.provenance().id(),
              new SerializedActivity(activityType, topic.outputType().serialize(input).asMap().orElseThrow()));
        });
      }

      private static <T>
      void extractOutput(final SerializableTopic<T> topic, final Event ev, final TaskInfo taskInfo) {
        if (!topic.name().startsWith("ActivityType.Output.")) return;

        ev.extract(topic.topic()).ifPresent(output -> {
          taskInfo.output.put(
              ev.provenance().id(),
              topic.outputType().serialize(output));
        });
      }
    }
  }

  private TaskInfo.Trait taskInfoTrait = null;
  public void updateTaskInfo(EventGraph<Event> g) {
    if (taskInfoTrait == null) taskInfoTrait = new TaskInfo.Trait(getMissionModel().getTopics(), defaultActivityTopic);
    g.evaluate(taskInfoTrait, taskInfoTrait::atom).accept(taskInfo);
  }

  public Map<String, ProfilingState<?>> generateResourceProfiles(final Duration simulationDuration) {
    if (resourceTracker != null) {
      resourceTracker.reset();
      // Replay the timeline to collect resource profiles
      while (!resourceTracker.isEmpty(simulationDuration, true)) {
        resourceTracker.updateResources(simulationDuration, true);
      }
      return resourceTracker.resourceProfiles();
    }

    return this.resources
               .entrySet()
               .stream()
               .collect(Collectors.toMap($ -> $.getKey().id(),
                                         Map.Entry::getValue));
  }

  /** Compute a set of results from the current state of simulation. */
  // TODO: Move result extraction out of the SimulationEngine.
  //   The Engine should only need to stream events of interest to a downstream consumer.
  //   The Engine cannot be cognizant of all downstream needs.
  // TODO: Whatever mechanism replaces `computeResults` also ought to replace `isTaskComplete`.
  // TODO: Produce results for all tasks, not just those that have completed.
  //   Planners need to be aware of failed or unfinished tasks.
  public SimulationResultsInterface computeResults(
      final Instant startTime,
      final Duration elapsedTime,
      final Topic<ActivityDirectiveId> activityTopic
  ) {
    if (debug) System.out.println("computeResults(startTime=" + startTime + ", elapsedTime=" + elapsedTime + "...) at time " + curTime());

//    if (resourceTracker != null) {
//      resourceTracker.reset();
//    }
    final Map<String, ProfilingState<?>> resources = generateResourceProfiles(elapsedTime);

    var serializableTopics = this.missionModel.getTopics();

    // Extract profiles for every resource.
    final var realProfiles = new HashMap<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>>();

    for (final var entry : resources.entrySet()) {
      final var name = entry.getKey();
      final var state = entry.getValue();

      final var resource = state.resource();

      switch (resource.getType()) {
        case "real" -> realProfiles.put(
            name,
            Pair.of(
                resource.getOutputType().getSchema(),
                serializeProfile(elapsedTime, state, SimulationEngine::extractRealDynamics)));

        case "discrete" -> discreteProfiles.put(
            name,
            Pair.of(
                resource.getOutputType().getSchema(),
                serializeProfile(elapsedTime, state, SimulationEngine::extractDiscreteDynamics)));

        default ->
            throw new IllegalArgumentException(
                "Resource `%s` has unknown type `%s`".formatted(name, resource.getType()));
      }
    }


    // Give every task corresponding to a child activity an ID that doesn't conflict with any root activity.
    this.taskToSimulatedActivityId = new HashMap<String, SimulatedActivityId>(taskInfo.taskToPlannedDirective.size());
    final var usedSimulatedActivityIds = new HashSet<>();
    for (final var entry : taskInfo.taskToPlannedDirective.entrySet()) {
      taskToSimulatedActivityId.put(entry.getKey(), new SimulatedActivityId(entry.getValue().id()));
      usedSimulatedActivityIds.add(entry.getValue().id());
    }
    long counter = 1L;
    for (final var task : this.tasks.keySet()) {
      if (!taskInfo.isActivity(task)) continue;
      if (taskToSimulatedActivityId.containsKey(task.id())) continue;

      while (usedSimulatedActivityIds.contains(counter)) counter++;
      taskToSimulatedActivityId.put(task.id(), new SimulatedActivityId(counter++));
    }

    // Identify the nearest ancestor *activity* (excluding intermediate anonymous tasks).
    final var activityParents = new HashMap<SimulatedActivityId, SimulatedActivityId>();
    this.tasks.forEach((task, state) -> {
      if (!taskInfo.isActivity(task)) return;

      var parent = this.taskParent.get(task);
      while (parent != null && !this.taskInfo.isActivity(parent)) {
        parent = this.taskParent.get(parent);
      }

      if (parent != null) {
        activityParents.put(taskToSimulatedActivityId.get(task.id()), taskToSimulatedActivityId.get(parent.id()));
      }
    });

    final var activityChildren = new HashMap<SimulatedActivityId, List<SimulatedActivityId>>();
    activityParents.forEach((task, parent) -> {
      activityChildren.computeIfAbsent(parent, $ -> new LinkedList<>()).add(task);
    });

    this.tasks.forEach((task, state) -> {
      if (!taskInfo.isActivity(task)) return;

      final var activityId = taskToSimulatedActivityId.get(task.id());
      final var directiveId = taskInfo.taskToPlannedDirective.get(task.id()); // will be null for non-directives

      if (state instanceof ExecutionState.Terminated<?> e) {
        final var inputAttributes = this.taskInfo.input().get(task.id());
        final var outputAttributes = this.taskInfo.output().get(task.id());

        this.simulatedActivities.put(activityId, new SimulatedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            e.joinOffset().minus(e.startOffset()),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.ofNullable(directiveId),
            outputAttributes
        ));
      } else if (state instanceof ExecutionState.InProgress<?> e){
        final var inputAttributes = this.taskInfo.input().get(task.id());
        this.unfinishedActivities.put(activityId, new UnfinishedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.ofNullable(directiveId)
        ));
      } else if (state instanceof ExecutionState.AwaitingChildren<?> e){
        final var inputAttributes = this.taskInfo.input().get(task.id());
        this.unfinishedActivities.put(activityId, new UnfinishedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.ofNullable(directiveId)
        ));
      } else {
        throw new Error("Unexpected subtype of %s: %s".formatted(ExecutionState.class, state.getClass()));
      }
    });

    final var serializableTopicToId = new HashMap<SerializableTopic<?>, Integer>();
    for (final var serializableTopic : serializableTopics) {
      serializableTopicToId.put(serializableTopic, this.topics.size());
      this.topics.add(Triple.of(this.topics.size(), serializableTopic.name(), serializableTopic.outputType().getSchema()));
    }

    // Serialize the timeline of EventGraphs
    for (Duration time: timeline.commitsByTime.keySet()) {
      var commitList = timeline.commitsByTime.get(time);
      for (var commit : commitList) {
        final var serializedEventGraph = commit.events().substitute(
            event -> {
              EventGraph<Pair<Integer, SerializedValue>> output = EventGraph.empty();
              for (final var serializableTopic : serializableTopics) {
                Optional<SerializedValue> serializedEvent = trySerializeEvent(event, serializableTopic);
                if (serializedEvent.isPresent()) {
                  output = EventGraph.concurrently(output, EventGraph.atom(Pair.of(serializableTopicToId.get(serializableTopic), serializedEvent.get())));
                }
              }
              return output;
            }
        ).evaluate(new EventGraph.IdentityTrait<>(), EventGraph::atom);
        if (!(serializedEventGraph instanceof EventGraph.Empty)) {
          this.serializedTimeline
              .computeIfAbsent(time, x -> new ArrayList<>())
              .add(serializedEventGraph);
        }
      }
    }

    this.simulationResults = new SimulationResults(realProfiles,
                                 discreteProfiles,
                                 this.simulatedActivities,
                                 this.removedActivities,
                                 this.unfinishedActivities,
                                 startTime,
                                 elapsedTime,
                                 this.topics,
                                 this.serializedTimeline);
    return getCombinedSimulationResults();
  }

  public SimulationResultsInterface getCombinedSimulationResults() {
    if (this.simulationResults == null ) {
      return computeResults(this.startTime, Duration.MAX_VALUE, defaultActivityTopic);
      //      return computeResults(this.startTime, curTime(), defaultActivityTopic);
    }
    if (oldEngine == null) {
      return this.simulationResults;
    }
    return new CombinedSimulationResults(this.simulationResults, oldEngine.getCombinedSimulationResults(), timeline);
  }

  public Optional<Duration> getTaskDuration(TaskId taskId){
    final var state = tasks.get(taskId);
    if (state instanceof ExecutionState.Terminated e) {
      return Optional.of(e.joinOffset().minus(e.startOffset()));
    }
    return Optional.empty();
  }

  private static <EventType> Optional<SerializedValue> trySerializeEvent(Event event, SerializableTopic<EventType> serializableTopic) {
    return event.extract(serializableTopic.topic(), serializableTopic.outputType()::serialize);
  }

  private interface Translator<Target> {
    <Dynamics> Target apply(Resource<Dynamics> resource, Dynamics dynamics);
  }

  private static <Target, Dynamics>
  List<ProfileSegment<Target>> serializeProfile(
      final Duration elapsedTime,
      final ProfilingState<Dynamics> state,
      final Translator<Target> translator
  ) {
    final var profile = new ArrayList<ProfileSegment<Target>>(state.profile().segments().size());

    final var iter = state.profile().segments().iterator();
    if (iter.hasNext()) {
      var segment = iter.next();
      while (iter.hasNext()) {
        final var nextSegment = iter.next();

        profile.add(new ProfileSegment<>(
            nextSegment.startOffset().minus(segment.startOffset()),
            translator.apply(state.resource(), segment.dynamics())));
        segment = nextSegment;
      }

      profile.add(new ProfileSegment<>(
          elapsedTime.minus(segment.startOffset()),
          translator.apply(state.resource(), segment.dynamics())));
    }

    return profile;
  }

  private static <Dynamics>
  RealDynamics extractRealDynamics(final Resource<Dynamics> resource, final Dynamics dynamics) {
    final var serializedSegment = resource.getOutputType().serialize(dynamics).asMap().orElseThrow();
    final var initial = serializedSegment.get("initial").asReal().orElseThrow();
    final var rate = serializedSegment.get("rate").asReal().orElseThrow();

    return RealDynamics.linear(initial, rate);
  }

  private static <Dynamics>
  SerializedValue extractDiscreteDynamics(final Resource<Dynamics> resource, final Dynamics dynamics) {
    return resource.getOutputType().serialize(dynamics);
  }

  /** A handle for processing requests from a modeled resource or condition. */
  public final class EngineQuerier<Job> implements Querier {
    private final Duration currentTime;
    public final TaskFrame<Job> frame;
    public final Set<Topic<?>> referencedTopics = new HashSet<>();
    private final Optional<Pair<Topic<Topic<?>>, TaskId>> queryTrackingInfo;
    public Optional<Duration> expiry = Optional.empty();

    public EngineQuerier(final Duration currentTime, final TaskFrame<Job> frame, final Topic<Topic<?>> queryTopic,
                         final TaskId associatedTask) {
      this.currentTime = currentTime;
      this.frame = Objects.requireNonNull(frame);
      this.queryTrackingInfo = Optional.of(Pair.of(Objects.requireNonNull(queryTopic), associatedTask));
    }

    public EngineQuerier(final Duration currentTime, final TaskFrame<Job> frame) {
      this.currentTime = currentTime;
      this.frame = Objects.requireNonNull(frame);
      this.queryTrackingInfo = Optional.empty();
    }

    @Override
    public <State> State getState(final CellId<State> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via MissionModelBuilder).
      @SuppressWarnings("unchecked")
      final var query = ((EngineCellId<?, State>) token);

      // find or create a cell for the query and step it up -- this used to be done in LiveCell.get()
      var cell = timeline.getCell(query.query(), currentTime, true);

      this.queryTrackingInfo.ifPresent(info -> {
        if (isTaskStale(info.getRight(), currentTime)) {
          // Create a noop event to mark when the read occurred in the EventGraph
          var noop = Event.create(info.getLeft(), query.topic(), info.getRight());
          this.frame.emit(noop);
          putInCellReadHistory(query.topic(), info.getRight(), noop, currentTime);
        }
      });

      this.expiry = min(this.expiry, cell.getExpiry());
      this.referencedTopics.add(query.topic());

      // TODO: Cache the state (until the query returns) to avoid unnecessary copies
      //  if the same state is requested multiple times in a row.
      final var state$ = cell.getState();

      return state$;
    }

    private static Optional<Duration> min(final Optional<Duration> a, final Optional<Duration> b) {
      if (a.isEmpty()) return b;
      if (b.isEmpty()) return a;
      return Optional.of(Duration.min(a.get(), b.get()));
    }
  }

  /** A handle for processing requests and effects from a modeled task. */
  private final class EngineScheduler implements Scheduler {
    private final Duration currentTime;
    private final TaskId activeTask;
    private final TaskFrame<JobId> frame;
    private final Topic<Topic<?>> queryTopic;

    public EngineScheduler(final Duration currentTime, final TaskId activeTask, final TaskFrame<JobId> frame, final Topic<Topic<?>> queryTopic) {
      this.currentTime = Objects.requireNonNull(currentTime);
      this.activeTask = Objects.requireNonNull(activeTask);
      this.frame = Objects.requireNonNull(frame);
      this.queryTopic = Objects.requireNonNull(queryTopic);
    }

    @Override
    public <State> State get(final CellId<State> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via MissionModelBuilder).
      @SuppressWarnings("unchecked")
      final var query = (EngineCellId<?, State>) token;

      // find or create a cell for the query and step it up -- this used to be done in LiveCell.get()
      var cell = timeline.getCell(query.query(), currentTime, true);

      // Don't emit a noop event for the read if the task is not yet stale.
      // The time that this task becomes stale was determined when it was created.
      if (isTaskStale(this.activeTask, currentTime)) {
        // TODO: REVIEW: What if the task becomes stale in the middle of a sequence of events within the same
        //       timepoint/EventGraph?  Should this be emitting an event in that case?
        //       Is there a problem of combining the existing or old EventGraph with a new one?

        // Create a noop event to mark when the read occurred in the EventGraph
        var noop = Event.create(queryTopic, query.topic(), activeTask);
        this.frame.emit(noop);
        putInCellReadHistory(query.topic(), activeTask, noop, currentTime);
      }

      // TODO: Cache the return value (until the next emit or until the task yields) to avoid unnecessary copies
      // if the same state is requested multiple times in a row.
      final var state$ = cell.getState();
      return state$;
    }

    @Override
    public <EventType> void emit(final EventType event, final Topic<EventType> topic) {
      if (debug) System.out.println("emit(): isTaskStale() --> " + isTaskStale(this.activeTask, this.currentTime));
      if (isTaskStale(this.activeTask, this.currentTime)) {
        // Add this event to the timeline.
        this.frame.emit(Event.create(topic, event, this.activeTask));
        if (debug) System.out.println("emit(): isTopicStale(" + topic + ") --> " + timeline.isTopicStale(topic, this.currentTime));
        if (!timeline.isTopicStale(topic, this.currentTime)) {
          SimulationEngine.this.timeline.setTopicStale(topic, this.currentTime);
        }
        SimulationEngine.this.invalidateTopic(topic, this.currentTime);
      }
    }

    /**
     * Return the taskId from the old simulation for the new (or old) TaskFactory.
     * @param taskFactory the TaskFactory used to create the task
     * @return the TaskId generated for the task created by taskFactory
     */
    public TaskId getOldTaskIdForDaemon(TaskFactory<?> taskFactory) {
      var taskId = oldEngine.getTaskIdForFactory(taskFactory);
      if (taskId != null) return taskId;
      String daemonId = getMissionModel().getDaemonId(taskFactory);
      if (daemonId == null) return null;
      var oldTaskFactory = oldEngine.getMissionModel().getDaemon(daemonId);
      if (oldTaskFactory == null) return null;
      taskId = oldEngine.getTaskIdForFactory(oldTaskFactory);
      return taskId;
    }

    @Override
    public void spawn(final TaskFactory<?> state) {
      final boolean rerunDaemonTask = oldEngine != null && getMissionModel().rerunDaemons();
      final boolean daemonTaskOrSpawn = daemonTasks.contains(this.activeTask) || getMissionModel().isDaemon(state);
      boolean settingTaskStale = rerunDaemonTask;
      // Don't spawn children of stale task unless it's a daemon task that is requested to be rerun.
      if (isTaskStale(this.activeTask, this.currentTime) || (rerunDaemonTask && daemonTaskOrSpawn)) {
        final TaskId task;
        if (rerunDaemonTask && getMissionModel().isDaemon(state)) {
          var tmpId = getOldTaskIdForDaemon(state); // Get TaskID from old simulation so that we can set it stale.
          if (tmpId != null) {
            task = tmpId;
          } else {
            // If we can't correlate the state (TaskFactory) to the daemon task run in the old simulation,
            // and the mission model says we need to re-run them (getMissionModel().rerunDaemons()), then
            // we rerun without removing the effects of the daemon on the past simulation, potentially
            // leading to bad behavior.
            task = TaskId.generate();
            settingTaskStale = false;
            System.err.println("WARNING: re-running daemon task as if never run before: " + task);
          }
        } else {
          task = TaskId.generate();
        }
        if (daemonTaskOrSpawn) {
          daemonTasks.add(task);
          if (settingTaskStale) {
            // Indicate that this task is not stale until after the time it last executed.
            var eventMap = getCombinedEventsByTask(task);
            var lastEventTimePlusE = eventMap == null ? null : eventMap.lastKey().plus(Duration.EPSILON);
            if (lastEventTimePlusE != null) {
              setTaskStale(task, lastEventTimePlusE);
            }
          }
        }
        // Record task information
        SimulationEngine.this.tasks.put(task, new ExecutionState.InProgress<>(this.currentTime, state.create(SimulationEngine.this.executor)));
        SimulationEngine.this.taskParent.put(task, this.activeTask);
        SimulationEngine.this.taskChildren.computeIfAbsent(this.activeTask, $ -> new HashSet<>()).add(task);
        SimulationEngine.this.taskFactories.put(task, state);
        SimulationEngine.this.taskIdsForFactories.put(state, task);
        this.frame.signal(JobId.forTask(task));
      }
    }
  }

  public static <E, T>
  TaskFactory<T> emitAndThen(final E event, final Topic<E> topic, final TaskFactory<T> continuation) {
    return executor -> scheduler -> {
      scheduler.emit(event, topic);
      return continuation.create(executor).step(scheduler);
    };
  }

  private boolean isActivity(final TaskId taskId) {
    if (this.taskInfo.isActivity(taskId)) return true;
    if (this.daemonTasks.contains(taskId)) return false;
    if (oldEngine == null) return false;
    return this.oldEngine.isActivity(taskId);
  }

  private TaskId getTaskParent(TaskId taskId) {
    var parent = this.taskParent.get(taskId);
    if (parent == null && oldEngine != null) {
      parent = oldEngine.getTaskParent(taskId);
    }
    return parent;
  }

  boolean isDaemonTask(TaskId taskId) {
    if (daemonTasks.contains(taskId)) return true;
    if (taskInfo.isActivity(taskId)) return false;
    if (oldEngine != null) {
      return oldEngine.isDaemonTask(taskId);
    }
    return false;
  }

  public ActivityDirectiveId getActivityDirectiveId(TaskId taskId) {
    var activityDirectiveId = taskInfo.taskToPlannedDirective.get(taskId.id());
    if (activityDirectiveId == null && oldEngine != null) {
      activityDirectiveId = oldEngine.getActivityDirectiveId(taskId);
    }
    return activityDirectiveId;
  }

  public ActivityDirective getActivityDirective(TaskId taskId) {
    var activityDirectiveId = getActivityDirectiveId(taskId);
    if (activityDirectiveId == null) return null;
    ActivityDirective directive = scheduledDirectives.get(activityDirectiveId);
    if (directive == null && oldEngine != null) {
      directive = oldEngine.getActivityDirective(taskId);
    }
    return directive;
  }

  public SerializedActivity getSerializedActivity(TaskId taskId) {
    SerializedActivity serializedActivity = this.taskInfo.input.get(taskId.id());
    if (serializedActivity == null && oldEngine != null) {
      serializedActivity = oldEngine.getSerializedActivity(taskId);
    }
    return serializedActivity;
  }

  public String getActivityTypeName(TaskId taskId) {
    SerializedActivity act = getSerializedActivity(taskId);
    if (act != null) return act.getTypeName();
    var directive = getActivityDirective(taskId);
    if (directive != null) {
      return directive.serializedActivity().getTypeName();
    }
    return null;
  }

  public String getNameForTask(TaskId taskId) {
    if (isDaemonTask(taskId)) {
      TaskFactory<?> factory = getFactoryForTaskId(taskId);
      if (factory == null) {
        return "unknown daemon task";
      }
      String daemonId = missionModel.getDaemonId(factory);
      if (daemonId == null) return "unknown daemon task";
      return daemonId;
    }
    if (isActivity(taskId)) {
      String name = getActivityTypeName(taskId);
      if (name != null) return name;
      return "unknown activity";
    }
    return "unknown task";
  }

  public Set<TaskId> getTaskChildren(TaskId taskId) {
    var children = this.taskChildren.get(taskId);
    if (children == null && oldEngine != null) {
      children = oldEngine.getTaskChildren(taskId);
    }
    return children;
  }

  public void rescheduleTask(TaskId taskId, Duration startOffset) {
    //Look for serialized activity for task
    // If no parent is an activity, then see if it is a daemon task.
    // If it's not an activity or daemon task, report an error somehow (e.g., exception or log.error()).
//    TaskId activityId = null;
//    TaskId daemonTaskId = taskId;
//    TaskId lastId = taskId;
//    boolean isAct = false;
//    boolean isDaemon = false;
//    while (true) {
//      if (oldEngine.isActivity(lastId)) {
//        isAct = true;
//        activityId = lastId;
//        isDaemon = false;
//        break;
//      }
//      if (oldEngine.isDaemonTask(lastId)) {
//        isDaemon = true;
//        daemonTaskId = lastId;
//        break;
//      }
//      if (oldEngine.getFactoryForTaskId(lastId) != null) {
//        break;
//      }
//      var tempId = oldEngine.getTaskParent(lastId);
//      if (tempId == null) {
//        break;
//      }
//      lastId = tempId;
//    }

    if (oldEngine.isDaemonTask(taskId)) {
      TaskFactory<?> factory = oldEngine.getFactoryForTaskId(taskId);
      if (factory != null && startOffset != null && startOffset != Duration.MAX_VALUE) {
        scheduleTask(startOffset, factory, taskId);  // TODO: Emit something like with emitAndThen() in the isAct case below?
      } else {
        String daemonId = missionModel.getDaemonId(factory);
        throw new RuntimeException("Can't reschedule daemon task " + daemonId + " (" + taskId + ") at time offset " + startOffset +
                                   (factory == null ? " because there is no TaskFactory." : "."));
      }
    } else if (oldEngine.isActivity(taskId)) {
      // Get the SerializedActivity for the taskId.
      // If an activity is found, see if it is associated with a directive and, if so, use the directive instead.
      SerializedActivity serializedActivity = this.taskInfo.input.get(taskId.id());
      var activityDirectiveId = taskInfo.taskToPlannedDirective.get(taskId.id());
      SimulatedActivity simulatedActivity = simulatedActivities.get(activityDirectiveId);
      if (startOffset == null || startOffset == Duration.MAX_VALUE) {
        if (simulatedActivity != null) {
          Instant actStart = simulatedActivity.start();
          startOffset = Duration.minus(actStart, this.startTime);
        } else {
          throw new RuntimeException("No SimulatedActivity for ActivityDirectiveId, " + activityDirectiveId);
        }
      }
      TaskFactory<?> task;
      try {
        task = missionModel.getTaskFactory(serializedActivity);
      } catch (InstantiationException ex) {
        // All activity instantiations are assumed to be validated by this point
        throw new Error("Unexpected state: activity instantiation %s failed with: %s"
                            .formatted(serializedActivity.getTypeName(), ex.toString()));
      }
      // TODO: What if there is no activityDirectiveId?
      scheduleTask(startOffset, emitAndThen(activityDirectiveId, defaultActivityTopic, task), taskId);
    } else {
      // We have a TaskFactory even though it's not an activity or daemon -- maybe a cached TaskFactory to avoid rerunning parents
      TaskFactory<?> factory = oldEngine.getFactoryForTaskId(taskId);
      if (factory != null && startOffset != null && startOffset != Duration.MAX_VALUE) {
        scheduleTask(startOffset, factory, taskId);  // TODO: Emit something like with emitAndThen() in the isAct case below?
      } else {
        throw new RuntimeException("Can't reschedule task " + taskId + " at time offset " + startOffset +
                                   (factory == null ? " because there is no TaskFactory." : "."));
      }
    }
  }

  /** A representation of a job processable by the {@link SimulationEngine}. */
  public sealed interface JobId {
    /** A job to step a task. */
    record TaskJobId(TaskId id) implements JobId {}

    /** A job to step all tasks waiting on a signal. */
    record SignalJobId(SignalId id) implements JobId {}

    /** A job to query a resource. */
    record ResourceJobId(ResourceId id) implements JobId {}

    /** A job to check a condition. */
    record ConditionJobId(ConditionId id) implements JobId {}

    static TaskJobId forTask(final TaskId task) {
      return new TaskJobId(task);
    }

    static SignalJobId forSignal(final SignalId signal) {
      return new SignalJobId(signal);
    }

    static ResourceJobId forResource(final ResourceId resource) {
      return new ResourceJobId(resource);
    }

    static ConditionJobId forCondition(final ConditionId condition) {
      return new ConditionJobId(condition);
    }
  }

  /** The lifecycle stages every task passes through. */
  private sealed interface ExecutionState<Return> {
    Duration startOffset();

    /** The task is in its primary operational phase. */
    record InProgress<Return>(Duration startOffset, Task<Return> state)
        implements ExecutionState<Return>
    {
      public AwaitingChildren<Return> completedAt(
          final Duration endOffset,
          final LinkedList<TaskId> remainingChildren) {
        return new AwaitingChildren<>(this.startOffset, endOffset, remainingChildren);
      }

      public InProgress<Return> continueWith(final Task<Return> newState) {
        return new InProgress<>(this.startOffset, newState);
      }
    }

    /** The task has completed its primary operation, but has unfinished children. */
    record AwaitingChildren<Return>(
        Duration startOffset,
        Duration endOffset,
        LinkedList<TaskId> remainingChildren
    ) implements ExecutionState<Return>
    {
      public Terminated<Return> joinedAt(final Duration joinOffset) {
        return new Terminated<>(this.startOffset, this.endOffset, joinOffset);
      }
    }

    /** The task and all its delegated children have completed. */
    record Terminated<Return>(
        Duration startOffset,
        Duration endOffset,
        Duration joinOffset
    ) implements ExecutionState<Return> {}
  }

//  public static Duration getEndOffset(ExecutionState<?> s) {
//    if (s instanceof ExecutionState.InProgress<?> ip) {
//      ip.
//      return null;
//    }
//  }
}
