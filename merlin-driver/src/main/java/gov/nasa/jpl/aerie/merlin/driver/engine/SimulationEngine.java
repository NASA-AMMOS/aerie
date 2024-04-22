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
import gov.nasa.jpl.aerie.merlin.protocol.types.SubInstantDuration;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
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
  private static boolean debug = false;
  private static boolean trace = false;

  /** The engine from a previous simulation, which we will leverage to avoid redundant computation */
  public final SimulationEngine oldEngine;

  /** The EventGraphs separated by Durations between the events */
  public final TemporalEventSource timeline;
  private LiveCells cells;
  /** The set of all jobs waiting for time to pass. */
  private final JobSchedule<JobId, SchedulingInstant> scheduledJobs = new JobSchedule<>();
  /** The set of all jobs waiting on a condition. */
  private final Map<ConditionId, TaskId> waitingTasks = new HashMap<>();
  /** The set of all tasks blocked on some number of subtasks. */
  private final Map<TaskId, MutableInt> blockedTasks = new HashMap<>();
  /** The set of conditions depending on a given set of topics. */
  private final Subscriptions<Topic<?>, ConditionId> waitingConditions = new Subscriptions<>();
  /** The set of queries depending on a given set of topics. */
  private final Subscriptions<Topic<?>, ResourceId> waitingResources = new Subscriptions<>();
  /** The topics referenced (cells read) by the last computation of the resource. */
  private HashMap<ResourceId, Set<Topic<?>>> referencedTopics = new HashMap<>();
  /** Separates generation of resource profile results from other parts of the simulation */
  public ResourceTracker resourceTracker;
  /** The history of when tasks read topics/cells */
  private final HashMap<Topic<?>, TreeMap<SubInstantDuration, HashMap<TaskId, Event>>> cellReadHistory = new HashMap<>();
  private final TreeMap<SubInstantDuration, HashSet<TaskId>> removedCellReadHistory = new TreeMap<>();

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
//  private boolean overlayingEvents = false;

  public Map<ActivityDirectiveId, ActivityDirective> scheduledDirectives = null;
  public Map<String, Map<ActivityDirectiveId, ActivityDirective>> directivesDiff = null;

  public final SpanInfo spanInfo = new SpanInfo(this);
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
  private HashMap<SpanId, SpanId> activityParents = null;
  private HashMap<SpanId, List<SpanId>> activityChildren = null;
  private HashMap<SpanId, ActivityDirectiveId> activityDirectiveIds = null;


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
  private final Map<TaskId, SubInstantDuration> staleTasks = new HashMap<>();
  private final Map<TaskId, Event> staleEvents = new HashMap<>();
  private final Map<TaskId, Integer> staleCausalEventIndex = new HashMap<>();

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
  public final Map<ResourceId, ProfilingState<?>> resources = new HashMap<>();

  /** The task that spawned a given task (if any). */
  private final Map<TaskId, TaskId> taskParent = new HashMap<>();
  /** The set of children for each task (if any). */
  @DerivedFrom("taskParent")
  private final Map<TaskId, Set<TaskId>> taskChildren = new HashMap<>();

  /** The set of all spans of work contributed to by modeled tasks. */
  private final Map<SpanId, Span> spans = new HashMap<>();
  /** A count of the direct contributors to each span, including child spans and tasks. */
  private final Map<SpanId, MutableInt> spanContributorCount = new HashMap<>();
  private Map<TaskId, SpanId> taskToSpanMap = new HashMap<>();
  private Map<SpanId, SequencedSet<TaskId>> spanToTaskMap = new HashMap<>();

  private HashMap<SpanId, SimulatedActivityId> spanToSimulatedActivityId = null;

  private HashMap<ActivityDirectiveId, SimulatedActivityId> directiveToSimulatedActivityId = new HashMap<>();

  /** A thread pool that modeled tasks can use to keep track of their state between steps. */
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  /**  */
  public void putInCellReadHistory(Topic<?> topic, TaskId taskId, Event noop, SubInstantDuration time) {
    // TODO: Can't we just get this from eventsByTopic instead of having a separate data structure?
    var inner = cellReadHistory.computeIfAbsent(topic, $ -> new TreeMap<>());
    inner.computeIfAbsent(time, $ -> new HashMap<>()).put(taskId, noop);
  }

  /**
   * A cache of the combinedHistory so that it does not need to be recomputed after simulation.  The parent engine sets
   * the cache for the child engine per topic and clears it for the grandchild per topic.  This assumes that an engine
   * will not have more than one parent.
   */
  protected HashMap<Topic<?>, TreeMap<SubInstantDuration, HashMap<TaskId, Event>>> _combinedHistory = new HashMap<>();
  /**
   * A cache of part of the combinedHistory computation that is the old combined history without the removed task history.
   * This should be cleared by the parent engine.
   */
  protected HashMap<Topic<?>, TreeMap<SubInstantDuration, HashMap<TaskId, Event>>> _oldCleanedHistory = new HashMap<>();
  // protected Duration _combinedHistoryTime = null;

//  public HashMap<Topic<?>, TreeMap<Duration, HashMap<TaskId, Event>>> getCombinedCellReadHistory() {
//
//  }
//  public TreeMap<Duration, HashMap<TaskId, Event>> getCombinedCellReadHistory(Topic<?> topic) {
//    return getCombinedCellReadHistory().get(topic);
//  }

  private static TreeMap<SubInstantDuration, HashMap<TaskId, Event>> _emptyTreeMap = new TreeMap<>();

  public TreeMap<SubInstantDuration, HashMap<TaskId, Event>> getCombinedCellReadHistory(Topic<?> topic) {
    // check cache
    var inner = _combinedHistory.get(topic);
    if (inner != null) return inner;

    inner = cellReadHistory.get(topic);
    if (oldEngine == null) {
      // If there's no history from an old engine, then just set the cache to the local history
      _combinedHistory = cellReadHistory;
      if (inner == null) return _emptyTreeMap;
      return inner;
    }

    var oldInner = oldEngine.getCombinedCellReadHistory(topic);
    if (oldInner == null) oldInner = _emptyTreeMap;
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
      Set<SubInstantDuration> commonKeys = oldInner.keySet().stream().filter(d -> removedCellReadHistory.containsKey(d)).collect(
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
    TreeMap<SubInstantDuration, HashMap<TaskId, Event>> combinedTopicHistory = null;
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
  public Pair<SubInstantDuration, Map<TaskId, HashSet<Pair<Topic<?>, Event>>>> earliestStaleReadsNew(SubInstantDuration after, SubInstantDuration before, Topic<Topic<?>> queryTopic) {
    // We need to have the reads sorted according to the event graph.  Currently, this function doesn't
    // handle a task reading a cell more than once in a graph.  But, we should make sure we handle this case. TODO
    var earliest = before;
    final var tasks = new HashMap<TaskId, HashSet<Pair<Topic<?>, Event>>>();
    ConcurrentSkipListSet<SubInstantDuration> durs = timeline.staleTopics.entrySet().stream().collect(ConcurrentSkipListSet::new,
                                                                                            (set, entry) -> set.addAll(entry.getValue().keySet().stream().filter(d -> entry.getValue().get(d)).toList()),
                                                                                            (set1, set2) -> set1.addAll(set2));
    if (durs.isEmpty()) return Pair.of(SubInstantDuration.MAX_VALUE, Collections.emptyMap());
    var earliestStaleTopic = durs.higher(after);
    final TreeMap<Duration, List<EventGraph<Event>>> readEvents = oldEngine.timeline.getCombinedEventsByTopic().get(queryTopic);
    if (readEvents == null || readEvents.isEmpty()) return Pair.of(SubInstantDuration.MAX_VALUE, Collections.emptyMap());
    var readEventsSubmap = readEvents.subMap(after.duration(), false, before.duration(), true);
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

    if (readEvents.isEmpty()) return Pair.of( SubInstantDuration.MAX_VALUE, Collections.emptyMap());
    for (var entry : timeline.staleTopics.entrySet()) {
      Topic<?> topic = entry.getKey();
      var subMap = entry.getValue().subMap(after, false, earliest, true);
      SubInstantDuration d = null;
      for (var e : subMap.entrySet()) {
        if (e.getValue()) {
          d = e.getKey();
          var topicEventsSubMap = readEventsSubmap.subMap(d.duration(), true, earliest.duration(), true);
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
    if (tasks.isEmpty()) earliest = SubInstantDuration.MAX_VALUE;
    return Pair.of(earliest, tasks);
  }

//public String whatsThis(Topic<?> topic) {
//    return missionModel.getResources().entrySet().stream().filter(e -> e.getValue().toString()).findFirst()
//}


  public Pair<SubInstantDuration, Map<TaskId, HashSet<Pair<Topic<?>, Event>>>> earliestStaleReads(SubInstantDuration after, SubInstantDuration before) {
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
      NavigableMap<SubInstantDuration, HashMap<TaskId, Event>> topicReadsAfter =
          topicReads.subMap(after, false, earliest, true);
      if (topicReadsAfter == null || topicReadsAfter.isEmpty()) {
        continue;
      }
      for (var entry : topicReadsAfter.entrySet()) {
        var d = entry.getKey();
        HashMap<TaskId, Event> taskIds = new HashMap<>();
        // Don't include tasks which are being re-executed
        for (var e :  entry.getValue().entrySet()) {
          if (!staleTasks.containsKey(e.getKey())) {
            taskIds.put(e.getKey(), e.getValue());
          }
        }
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
    if (tasks.isEmpty()) earliest = SubInstantDuration.MAX_VALUE;
    return Pair.of(earliest, tasks);
  }

  /**
   * Get the earliest time that stale topics have events in the old simulation.  These are places where we need
   * to update resource profiles but that aren't captured by {@link #earliestStaleTopics(SubInstantDuration, SubInstantDuration)}.
   */
  public Pair<List<Topic<?>>, SubInstantDuration> nextStaleTopicOldEvents(SubInstantDuration after, SubInstantDuration before) {
    var list = new ArrayList<Topic<?>>();
    var earliest = before;
    for (var entry : timeline.staleTopics.entrySet()) {
      Topic<?> topic = entry.getKey();
      Optional<SubInstantDuration> nextStale = timeline.whenIsTopicStale(topic, after.plus(1), before);
      if (nextStale.isEmpty()) continue;
      TreeMap<Duration, List<EventGraph<Event>>> eventsByTime =
          timeline.oldTemporalEventSource.getCombinedEventsByTopic().get(topic);
      if (eventsByTime == null) continue;
      if (nextStale.get().longerThan(earliest)) continue;
      var subMap = eventsByTime.subMap(nextStale.get().duration(), true, earliest.duration(), true);
      SubInstantDuration time = null;
      for (var e : subMap.entrySet()) {
        Duration d = e.getKey();
        final List<EventGraph<Event>> events = e.getValue();
        if (events == null || events.isEmpty()) continue;
//        int step = d.isEqualTo(after.duration()) ? after.index() : 0;
        int step = d.isEqualTo(nextStale.get().duration()) ? nextStale.get().index() : 0;
        int maxSteps = Math.min(events.size(), before.duration().isEqualTo(nextStale.get().duration()) ? before.index() : Integer.MAX_VALUE);
        for (; step < maxSteps; ++step) {
          var graph = events.get(step);
          if (timeline.oldTemporalEventSource.getTopicsForEventGraph(graph).contains(topic)) {
            time = new SubInstantDuration(d, step);
            if (time.longerThan(after) && timeline.isTopicStale(topic, time) ) {
              break;
            }
            time = null;
          }
        }
        if (time != null) break;
      }
      if (time == null) {
        continue;
      }
      int comp = time.compareTo(earliest);
      if (comp <= 0) {
        if (comp < 0) list.clear();
        list.add(topic);
        earliest = time;
      }
    }
    if (list.isEmpty()) earliest = SubInstantDuration.MAX_VALUE;
    return Pair.of(list, earliest);
  }

  /** Get the earliest time that topics become stale and return those topics with the time */
  public Pair<List<Topic<?>>, SubInstantDuration> earliestStaleTopics(SubInstantDuration after, SubInstantDuration before) {
    if (before.noLongerThan(after)) {
      return Pair.of(Collections.emptyList(), SubInstantDuration.MAX_VALUE);
    }
    var list = new ArrayList<Topic<?>>();
    var earliest = before;
    for (var entry : timeline.staleTopics.entrySet()) {
      Topic<?> topic = entry.getKey();
      var subMap = entry.getValue().subMap(after, false, earliest, true);
      SubInstantDuration d = null;
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
    if (list.isEmpty()) earliest = SubInstantDuration.MAX_VALUE;
    return Pair.of(list, earliest);
  }

  public Pair<List<Topic<?>>, SubInstantDuration> earliestConditionTopics(SubInstantDuration after, SubInstantDuration before) {
    if (before.noLongerThan(after)) {
      return Pair.of(Collections.emptyList(), SubInstantDuration.MAX_VALUE);
    }
    var list = new ArrayList<Topic<?>>();
    var earliest = before;
    for (Topic topic : this.waitingConditions.getTopics()) {
      TreeMap<Duration, List<EventGraph<Event>>> eventsByTime =
          timeline.getCombinedEventsByTopic().get(topic);
      if (eventsByTime == null) continue;
      var subMap = eventsByTime.subMap(after.duration(), false, earliest.duration(), true);
      SubInstantDuration time = null;
      for (var e : subMap.entrySet()) {
        final List<EventGraph<Event>> events = e.getValue();
        if (events == null || events.isEmpty()) continue;
        Duration d = e.getKey();
        for (int step = 0; step < events.size(); ++step) {
          var graph = events.get(step);
          var topicForGraph = getTopicsForEventGraph(graph);
          if (topicForGraph.contains(topic)) {
            time = new SubInstantDuration(d, step);
//            if (timeline.isTopicStale(topic, time)) {
              break;
//            }
//            time = null;
          }
        }
        if (time != null) break;
      }
      if (time == null) {
        continue;
      }
      int comp = time.compareTo(earliest);
      if (comp <= 0) {
        if (comp < 0) list.clear();
        list.add(topic);
        earliest = time;
      }
    }
    if (list.isEmpty()) earliest = SubInstantDuration.MAX_VALUE;
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
   *
   * @param taskId id of the task being set stale
   * @param time time when the task becomes stale
   * @param afterEvent
   */
  public void setTaskStale(TaskId taskId, SubInstantDuration time, final Event afterEvent) {
    var staleTime = staleTasks.get(taskId);
    if (staleTime != null) {
      if (staleTime.shorterThan(time) || (staleTime.isEqualTo(time) &&
                                          (afterEvent == null || staleEvents.get(taskId) == null ||
                                           eventPrecedes(afterEvent, staleEvents.get(taskId), time)))) {
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
      staleEvents.put(taskId, afterEvent);
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
    removeTaskHistory(parentId, time, afterEvent);
  }

  private boolean eventPrecedes(Event e1, Event e2, SubInstantDuration time) {
    if (e1 == null || e2 == null || time == null) return false;
    List<TemporalEventSource.TimePoint.Commit> commits = timeline.getCombinedCommitsByTime().get(time.duration());
    var commit = commits.get(time.index());
    final Pair<EventGraph<Event>, Boolean> pair = commit.events().filter(e -> e == e2, e1, false);
    if (pair.getRight() && pair.getLeft().countNonEmpty() > 0) {
      return true;
    }
    return false;
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
  public void rescheduleStaleTasks(Pair<SubInstantDuration, Map<TaskId, HashSet<Pair<Topic<?>, Event>>>> earliestStaleReads) {
    if (debug) System.out.println("rescheduleStaleTasks(" + earliestStaleReads + ")");
    // Test to see if read value has changed.  If so, reschedule the affected task
    var timeOfStaleReads = earliestStaleReads.getLeft();
    for (Map.Entry<TaskId, HashSet<Pair<Topic<?>, Event>>> entry : earliestStaleReads.getRight().entrySet()) {
      final var taskId = entry.getKey();
      for (Pair<Topic<?>, Event> pair : entry.getValue()) {
        final var topic = pair.getLeft();
        final var noop = pair.getRight();
        // Need to step cell up to the point of the read
        // First, step up the cell to the time before the event graph where the read takes place and then
        // make a duplicate of the cell since partial evaluation of an event graph makes the cell unusable
        // for stepping further.
        Cell<?> steppedCell = timeOfStaleReads.index() > 0 ?
                              timeline.getCell(topic, new SubInstantDuration(timeOfStaleReads.duration(),
                                                                             timeOfStaleReads.index()-1)) :
                              timeline.liveCells.getCells(topic).stream().findFirst().orElseThrow().cell;
        if (debug) System.out.println("rescheduleStaleTasks(): steppedCell = " + steppedCell + ", cell time = " + timeline.getCellTime(steppedCell));
        final Cell<?> tempCell = steppedCell.duplicate();
        timeline.putCellTime(tempCell,timeline.getCellTime(steppedCell));
        timeline.stepUp(tempCell, timeOfStaleReads, noop);
        timeline.putCellTime(tempCell, null);

        Cell<?> oldCell = timeOfStaleReads.index() > 0 ?
                          timeline.oldTemporalEventSource.getCell(topic, new SubInstantDuration(timeOfStaleReads.duration(),
                                                                                                timeOfStaleReads.index()-1)) :
                          timeline.oldTemporalEventSource.liveCells.getCells(topic).stream().findFirst().orElseThrow().cell;
        if (debug) System.out.println("rescheduleStaleTasks(): oldCell = " + oldCell + ", cell time = " + timeline.oldTemporalEventSource.getCellTime(oldCell));
        final Cell<?> tempOldCell = oldCell.duplicate();
        timeline.oldTemporalEventSource.putCellTime(tempOldCell,timeline.oldTemporalEventSource.getCellTime(oldCell));
        timeline.oldTemporalEventSource.stepUp(tempOldCell, timeOfStaleReads, noop);
        timeline.oldTemporalEventSource.putCellTime(tempOldCell, null);

          if (!tempCell.getState().equals(tempOldCell.getState())) {
            if (debug) System.out.println("rescheduleStaleTasks(): Stale read: new cell state (" + tempCell + ") != old cell state (" + tempOldCell + ")");
            // Mark stale and reschedule task
            setTaskStale(taskId, timeOfStaleReads, noop);
            break;  // rescheduled task, so can move on to the next task
          }
//        }
      }  // for Pair<Topic<?>, Event>
    }  // for Map.Entry<TaskId, HashSet<Pair<Topic<?>, Event>>>
  }


  public SpanId putSpanId(TaskId taskId, SpanId spanId) {
    spanToTaskMap.computeIfAbsent(spanId, $ -> new LinkedHashSet<>()).add(taskId);
    return taskToSpanMap.put(taskId, spanId);
  }
  public SpanId getSpanId(TaskId taskId) {
    var s = taskToSpanMap.get(taskId);
    if (s == null && oldEngine != null) {
      return oldEngine.getSpanId(taskId);  // TODO -- do we need caches to avoid walking a long chain of oldEngines?
    }
    return s;
  }
  public SequencedSet<TaskId> getTaskIds(final SpanId spanId) {
    var s = spanToTaskMap.get(spanId);
    SpanId sId = spanId;
    while (s == null) {
      final Span span = spans.get(sId);
      if (span != null) {
        if (span.parent().isPresent()) {
          sId = span.parent().get();
          s = spanToTaskMap.get(sId);
        } else {
          break;
        }
      } else {
        break;
      }
    }
    if (s == null && oldEngine != null) {
      return oldEngine.getTaskIds(spanId);  // TODO -- do we need caches to avoid walking a long chain of oldEngines?
    }
    return s;
  }

  public TaskId getTaskIdForDirectiveId(ActivityDirectiveId id) {
    var spanId = getSpanIdForDirectiveId(id);
    SequencedSet<TaskId> taskIds = null;
    TaskId taskId = null;
    if (spanId != null) {
      taskIds = getTaskIds(spanId);
      if (taskIds != null && !taskIds.isEmpty()) {
        taskId = taskIds.getFirst();
      }
    }
    if (taskId == null && oldEngine != null) {
      taskId = oldEngine.getTaskIdForDirectiveId(id);  // TODO -- do we need caches to avoid walking a long chain of oldEngines?
      // NOTE -- We do not need filter out removed tasks because the directive would also be removed, in which case we do want the removed task.
    }
    return taskId;
  }

  public SimulatedActivityId getSimulatedActivityIdForDirectiveId(ActivityDirectiveId directiveId) {
    SimulatedActivityId simId = null;
    if (directiveToSimulatedActivityId != null) {
      simId = directiveToSimulatedActivityId.get(directiveId);
    }
    if (simId == null && oldEngine != null) {
      simId = oldEngine.getSimulatedActivityIdForDirectiveId(directiveId);
    }
    return simId;
  }

  public SpanId getSpanIdForDirectiveId(ActivityDirectiveId id) {
    var spanId = this.spanInfo.getSpanIdForDirectiveId(id);
    if (spanId == null && oldEngine != null) {
      spanId = oldEngine.getSpanIdForDirectiveId(id);  // TODO -- do we need caches to avoid walking a long chain of oldEngines?
    }
    return spanId;
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

  private Set<Topic<?>> getTopicsForEventGraph(EventGraph graph) {
    var r = this.timeline.topicsForEventGraph.get(graph);
    if (r == null && oldEngine != null) {
      r = oldEngine.getTopicsForEventGraph(graph);
    }
    if (r == null) return Collections.emptySet();
    return r;
  }

  private TreeMap<Duration, List<EventGraph<Event>>> getCombinedEventsByTask(TaskId taskId) {
    var newEvents = this.timeline.eventsByTask.get(taskId);
    if (oldEngine == null) return newEvents;
    SimulationEngine engine = this;
    ArrayList<SimulationEngine> engines = new ArrayList<>();
    TreeMap<Duration, List<EventGraph<Event>>> oldEvents = null;
    // Find the shallowest engine that saved old events
    while (engine != null) {
      engines.add(engine);
      if (engine._oldEventsByTask.containsKey(taskId)) {
        oldEvents = engine._oldEventsByTask.get(taskId);
        if (engine != this) engine._oldEventsByTask.remove(taskId); // purge old caches being replaced by new cache
        break;
      }
      engine = engine.oldEngine;
    }
    // Walk backwards, combining graphs
    for (int i=engines.size()-1; i >= 0; --i) {
      engine = engines.get(i);
      if (i == 0) engine._oldEventsByTask.put(taskId, oldEvents); // only update this engine's cache
      newEvents = engine.timeline.eventsByTask.get(taskId);
      var tmp_old = TemporalEventSource.mergeMapsFirstWins(newEvents, oldEvents);
      oldEvents = tmp_old;
    }
    return oldEvents;
  }
  private HashMap<TaskId, TreeMap<Duration, List<EventGraph<Event>>>> _oldEventsByTask = new HashMap<>();


  // TODO -- make recursive calls here non-recursive (like in getCombinedEventsByTask()),
  // TODO -- including getSimulatedActivityIdForTaskId(), setCurTime(), and CombinedSimulationResults

  //private HashSet<TaskId> _missingOldSimulatedActivityIds = new HashSet<>(); // short circuit deeply nested searches for taskIds that have
  private SimulatedActivityId getSimulatedActivityIdForTaskId(TaskId taskId) {
    //if (_missingOldSimulatedActivityIds.contains(taskId)) return
    SimulatedActivityId simId = null;
    var spanId = getSpanId(taskId);
    if (spanId == null && oldEngine != null) {
      spanId = oldEngine.getSpanId(taskId);
    }
    if (spanId != null) {
      if (spanToSimulatedActivityId != null) {
        simId = spanToSimulatedActivityId.get(spanId);
      } else if (oldEngine != null && oldEngine.spanToSimulatedActivityId != null) {
        simId = oldEngine.spanToSimulatedActivityId.get(spanId);
      }
    }
    //var simId = taskToSimulatedActivityId == null ? null : taskToSimulatedActivityId.get(taskId.id());
    if (simId == null && oldEngine != null) {
      // If this activity hasn't been seen in this simulation, it may be in a past one; this check avoids unnecessarily recursing
      if (this.isActivity(taskId)) {
        simId = oldEngine.getSimulatedActivityIdForTaskId(taskId);
      }
    }
    return simId;
  }

  public void removeActivity(final ActivityDirectiveId directiveId) {
    var simId = getSimulatedActivityIdForDirectiveId(directiveId);
    if (simId == null) {
      throw new RuntimeException("Could not find SimulatedActivityId for ActivityDirectiveId, " + directiveId);
    }
    removedActivities.add(simId);
    TaskId taskId = getTaskIdForDirectiveId(directiveId);
    if (taskId == null) {
      throw new RuntimeException("Could not find TaskId for ActivityDirectiveId, " + directiveId);
    }
    removeTaskHistory(taskId, SubInstantDuration.MIN_VALUE, null);
  }

  public void removeTaskHistory(final TaskId taskId, SubInstantDuration startingAfterTime, Event afterEvent) { // TODO -- need graph index with time
    // Look for the task's Events in the old and new timelines.
    if (debug) System.out.println("removeTaskHistory(taskId=" + taskId + " : " + getNameForTask(taskId) + ", startingAfterTime=" + startingAfterTime + ", afterEvent=" + afterEvent + ") BEGIN");
    final TreeMap<Duration, List<EventGraph<Event>>> graphsForTask = this.timeline.eventsByTask.get(taskId);
    final TreeMap<Duration, List<EventGraph<Event>>> oldGraphsForTask = this.oldEngine.getCombinedEventsByTask(taskId);
    if (debug) System.out.println("old combined graphs = " + oldGraphsForTask);
    if (debug) System.out.println("new local graphs =    " + graphsForTask);
    if (debug) {
      final TreeMap<Duration, List<EventGraph<Event>>> combinedGraphsForTask = this.getCombinedEventsByTask(taskId);
      if (debug) System.out.println("new combined graphs = " + combinedGraphsForTask);
    }
    var allKeys = new TreeSet<Duration>();
    if (graphsForTask != null) {
      allKeys.addAll(graphsForTask.keySet());
    }
    if (oldGraphsForTask != null) {
      allKeys.addAll(oldGraphsForTask.keySet());
    }
    for (Duration time : allKeys.tailSet(startingAfterTime.duration(), true)) {
      //if (time.shorterThan(startingAfterTime.duration())) continue;
      List<EventGraph<Event>> gl = graphsForTask == null ? null : graphsForTask.get(time); // If old graph is already replaced used the replacement
      if (gl == null || gl.isEmpty()) gl = oldGraphsForTask == null ? null : oldGraphsForTask.get(time);  // else we can replace the old graph
      if (gl == null) continue;
      final int firstStep = time.isEqualTo(startingAfterTime.duration()) ? startingAfterTime.index() : 0;
//      if (afterEvent != null && (firstStep >= gl.size() || gl.get(firstStep).filter(e -> e == afterEvent).countNonEmpty() != 1)) {
//        //System.err.println("ERROR! Could not find event " + afterEvent + " in graph for index " + firstStep + " in " + gl);
//        throw new RuntimeException("Could not find event " + afterEvent + " in graph for index " + firstStep + " in " + gl);
//      }
      //if (debug) System.out.println("comparing old graphs replacing old graph=" + g + " with new graph=" + newG + " at time " + time);
      for (int step=firstStep; step < gl.size(); ++step) {
        var g = gl.get(step);
        SubInstantDuration staleTime = new SubInstantDuration(time, step);
//        // invalidate topics for cells affected by the task in the old graph so that resource values are checked at
//        // this time to erase effects on resources  -- TODO: this doesn't work!  only one scheduled job per resource
        var s = new HashSet<Topic<?>>();
        TemporalEventSource.extractTopics(s, g, e -> taskId.equals(e.provenance()));
        //s.forEach(topic -> invalidateTopic(topic, time));
        s.forEach(topic -> timeline.setTopicStale(topic, staleTime));
        // replace the old graph with one without the task's events, updating data structures
        var pair = g.filter(e -> !taskId.equals(e.provenance()),
                            step == firstStep && time.isEqualTo(startingAfterTime.duration()) ? afterEvent : null,
                            true);
        var newG = pair.getLeft();
        if (newG != g) {
          if (debug) System.out.println("replacing old graph=" + g + " with new graph=" + newG + " at time " + time);
          timeline.replaceEventGraph(g, newG);
          updateTaskInfo(newG);
          removedCellReadHistory.computeIfAbsent(staleTime, $ -> new HashSet<>()).add(taskId);
        }
      }
    }
    // remove span from spanInfo data structures
    SpanId spanId = getSpanId(taskId);
    if (spanId != null) spanInfo.removeSpan(spanId);

    // Remove children, too!
    var children = this.oldEngine.getTaskChildren(taskId);
    if (children != null) children.forEach(c -> removeTaskHistory(c, startingAfterTime, afterEvent));
    if (debug) {
      final TreeMap<Duration, List<EventGraph<Event>>> localGraphsForTask = this.timeline.eventsByTask.get(taskId);
      final TreeMap<Duration, List<EventGraph<Event>>> combinedGraphsForTask = this.getCombinedEventsByTask(taskId);
      System.out.println("resulting local graphs = " + localGraphsForTask);
      System.out.println("resulting combined graphs = " + combinedGraphsForTask);
    }
    if (debug) System.out.println("removeTaskHistory(taskId=" + taskId + " : " + getNameForTask(taskId) + ", startingAfterTime=" + startingAfterTime + ", afterEvent=" + afterEvent + ") END");
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
  public <Output> SpanId scheduleTask(final Duration startTime, final TaskFactory<Output> state, TaskId taskIdToUse) {
    if (startTime.isNegative()) throw new IllegalArgumentException("Cannot schedule a task before the start time of the simulation");

    SpanId spanIdToUse = taskIdToUse == null ? null : getSpanId(taskIdToUse);
    final var span = spanIdToUse == null ? SpanId.generate() : spanIdToUse;
    this.spans.put(span, new Span(Optional.empty(), startTime, Optional.empty()));

    final var task = taskIdToUse == null ? TaskId.generate() : taskIdToUse;
    this.spanContributorCount.put(span, new MutableInt(1));
    this.tasks.put(task, new ExecutionState<>(span, 0, Optional.empty(), state.create(this.executor), startTime));
    putSpanId(task, span);

    this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(startTime));

    return span;
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

  public boolean isTaskStale(TaskId taskId, SubInstantDuration timeOffset, long causalEventIndex) {
    final SubInstantDuration staleTime = this.staleTasks.get(taskId);
    if (staleTime == null) {
      return true;  // This is only asked of scheduled tasks, so if there is no stale time,
      // then the task must be new or modified by the user, so it should always be considered stale.
      // NOTE: In the case of a modified task, is it possible to predict that it will have no effect?
      // NOTE: No, even if only the start time changed, effects could depend on the start time.  A new interface would
      // NOTE: be needed to convey how to determine staleness.
    }
    if (staleTime.shorterThan(timeOffset)) return true;
//    if (staleTime.isEqualTo(timeOffset)) {
//      var staleEventIndex = this.staleCausalEventIndex.get(taskId);
//      if ()
//    }
    return staleTime.noLongerThan(timeOffset);
  }
  public boolean isTaskStale(TaskId taskId, SubInstantDuration timeOffset) {
    final SubInstantDuration staleTime = this.staleTasks.get(taskId);
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
      this.scheduledJobs.unschedule(JobId.forSignal(condition));
      var cjid = JobId.forCondition(condition);
      var t = SubInstant.Conditions.at(invalidationTime);
      if (trace) System.out.println("invalidateTopic(): schedule(ConditionJobId " + cjid + " at time " + t + ")");
      this.scheduledJobs.schedule(cjid, t);
    }
  }

  /** Returns the offset time of the next batch of scheduled jobs. */
  public SubInstantDuration timeOfNextJobs() {
    return this.scheduledJobs.timeOfNextJobs();
  }

  /** Removes and returns the next set of jobs to be performed concurrently. */
  public JobSchedule.Batch<JobId> extractNextJobs(final Duration maximumTime) {
    final var batch = this.scheduledJobs.extractNextJobs(maximumTime);

    // If we're signaling based on a condition, we need to untrack the condition before any tasks run.
    // Otherwise, we could see a race if one of the tasks running at this time invalidates state
    // that the condition depends on, in which case we might accidentally schedule an update for a condition
    // that no longer exists.
    for (final var job : batch.jobs()) {
      if (!(job instanceof JobId.SignalJobId s)) continue;

      this.conditions.remove(s.id());
      this.waitingConditions.unsubscribeQuery(s.id());
    }

    return batch;
  }

  /** Performs a collection of tasks concurrently, extending the given timeline by their stateful effects. */
  public void step(final Duration maximumTime,
                   final Consumer<Duration> simulationExtentConsumer) {
    if (debug) System.out.println("step(): begin -- time = " + curTime() + ", step " + stepIndexAtTime);
    if (stepIndexAtTime == Integer.MAX_VALUE) stepIndexAtTime = 0;
    var timeOfNextJobs = timeOfNextJobs();
    timeOfNextJobs = new SubInstantDuration(timeOfNextJobs().duration(), Math.max(timeOfNextJobs.index(), stepIndexAtTime));
    var nextTime = timeOfNextJobs;

    Pair<SubInstantDuration, Map<TaskId, HashSet<Pair<Topic<?>, Event>>>> earliestStaleReads = null;
    SubInstantDuration staleReadTime = null;
    Pair<List<Topic<?>>, SubInstantDuration> earliestStaleTopics = null;
    Pair<List<Topic<?>>, SubInstantDuration> earliestStaleTopicOldEvents = null;
    SubInstantDuration staleTopicTime = SubInstantDuration.MAX_VALUE;
    SubInstantDuration staleTopicOldEventTime = SubInstantDuration.MAX_VALUE;
    SubInstantDuration conditionTime = SubInstantDuration.MAX_VALUE;
    Pair<List<Topic<?>>, SubInstantDuration> earliestConditionTopics = null;

    if (oldEngine != null && nextTime.noShorterThan(curTime().duration())) {
      if (resourceTracker == null) {
        // Need to invalidate stale topics just after the event, so the time of the events returned must be incremented
        // by index=1, and the window searched must be 1 index before the current time.
        earliestStaleTopics = earliestStaleTopics(curTime().minus(1), nextTime);  // TODO: might want to not limit by nextTime and cache for future iterations
        if (debug) System.out.println("earliestStaleTopics(" + curTime().minus(1) + ", " + nextTime + ") = " + earliestStaleTopics);
        staleTopicTime = earliestStaleTopics.getRight().plus(1);
        nextTime = SubInstantDuration.min(nextTime, staleTopicTime);

        earliestStaleTopicOldEvents = nextStaleTopicOldEvents(curTime().minus(1), SubInstantDuration.min(nextTime, new SubInstantDuration(maximumTime, 0)));
        if (debug) System.out.println("nextStaleTopicOldEvents(" + curTime().minus(1) + ", " + SubInstantDuration.min(nextTime, new SubInstantDuration(maximumTime, 0)) + ") = " + earliestStaleTopicOldEvents);
        staleTopicOldEventTime = earliestStaleTopicOldEvents.getRight().plus(1);
        nextTime = SubInstantDuration.min(nextTime, staleTopicOldEventTime);
      }

      earliestStaleReads = earliestStaleReads(
          curTime(),
          nextTime);  // might want to not limit by nextTime and cache for future iterations
      staleReadTime = earliestStaleReads.getLeft();
      nextTime = SubInstantDuration.min(nextTime, staleReadTime);

      // Need to invalidate stale topics just after the event, so the time of the events returned must be incremented
      // by index=1, and the window searched must be 1 index before the current time.
      earliestConditionTopics = earliestConditionTopics(curTime().minus(1), nextTime);
      if (debug) System.out.println("earliestConditionTopics(" + curTime().minus(1) + ", " + nextTime + ") = " + earliestConditionTopics);
      conditionTime = earliestConditionTopics.getRight().plus(1);
      nextTime = SubInstantDuration.min(nextTime, conditionTime);
    }

    // Increment real time, if necessary.
    nextTime = SubInstantDuration.min(nextTime, new SubInstantDuration(maximumTime, Integer.MAX_VALUE));
//    var delta = timeForDelta.minus(curTime().duration());
//    if (!delta.isZero()) {
//      stepIndexAtTime = 0;
//    }
    setCurTime(nextTime);
    stepIndexAtTime = nextTime.index();
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
          invalidateTopic(topic, nextTime.duration());
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
          invalidateTopic(topic, nextTime.duration());
          invalidatedTopics.add(topic);
        }
      }

      if (conditionTime.isEqualTo(nextTime)) {
        //if (debug) System.out.println("earliestConditionTopics at " + nextTime + " = " + earliestConditionTopics);
        for (Topic<?> topic : earliestConditionTopics
            .getLeft()
            .stream()
            .filter(t -> !invalidatedTopics.contains(t))
            .toList()) {
          invalidateTopic(topic, nextTime.duration());
          invalidatedTopics.add(topic);
        }
      }
    }
    if (staleReadTime != null && staleReadTime.isEqualTo(nextTime)) {
      if (debug) System.out.println("earliestStaleReads at " + nextTime + " = " + earliestStaleReads);
      rescheduleStaleTasks(earliestStaleReads);
    } else
    if (timeOfNextJobs.isEqualTo(nextTime) && invalidatedTopics.isEmpty()) {

      final var batch = extractNextJobs(maximumTime);
      if (debug) System.out.println("step(): perform job batch at " + nextTime + " : " + batch.jobs().stream().map($ -> $.getClass()).toList());

      //setCurTime(batch.offsetFromStart());
      var tip = EventGraph.<Event>empty();
      for (final var job$ : batch.jobs()) {
        tip = EventGraph.concurrently(tip, TaskFrame.run(job$, this.cells, (job, frame) -> {
          this.performJob(job, frame, curTime(), maximumTime, missionModel.queryTopic);
        }));
      }

      if (!(tip instanceof EventGraph.Empty) ||
          (!batch.jobs().isEmpty() && batch.jobs().stream().findFirst().get() instanceof JobId.TaskJobId)) {
        this.timeline.add(tip, curTime().duration(), stepIndexAtTime, missionModel.queryTopic);
        //updateTaskInfo(tip);
        if (stepIndexAtTime < Integer.MAX_VALUE) stepIndexAtTime += 1;
        else throw new RuntimeException("Only Resource jobs (not Task jobs) should be run at step index Integer.MAX_VALUE");
      }
    }
    if (debug) System.out.println("step(): end -- time = " + curTime() + ", step " + stepIndexAtTime);
  }

  /** Performs a single job. */
  private void performJob(
      final JobId job,
      final TaskFrame<JobId> frame,
      final SubInstantDuration currentTime,
      final Duration maximumTime,
      final Topic<Topic<?>> queryTopic) {
    if (job instanceof JobId.TaskJobId j) {
      this.stepTask(j.id(), frame, currentTime, queryTopic);
    } else if (job instanceof JobId.SignalJobId j) {
      this.stepTask(this.waitingTasks.remove(j.id()), frame, currentTime, queryTopic);
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
  public void stepTask(final TaskId task, final TaskFrame<JobId> frame, final SubInstantDuration currentTime,
                       final Topic<Topic<?>> queryTopic) {
    // The handler for the next status of the task is responsible
    //   for putting an updated state back into the task set.
    var state = this.tasks.remove(task);

    stepEffectModel(task, state, frame, currentTime, queryTopic);
  }

  /** Make progress in a task by stepping its associated effect model forward. */
  private <Output> void stepEffectModel(
      final TaskId task,
      final ExecutionState<Output> progress,
      final TaskFrame<JobId> frame,
      final SubInstantDuration currentTime,
      final Topic<Topic<?>> queryTopic) {
    // Step the modeling state forward.
    final var scheduler = new EngineScheduler(currentTime, progress.shadowedSpans(), task, progress.span(), progress.caller(), frame, queryTopic);
    final var status = progress.state().step(scheduler);

    // TODO: Report which topics this activity wrote to at this point in time. This is useful insight for any user.
    // TODO: Report which cells this activity read from at this point in time. This is useful insight for any user.

    // Based on the task's return status, update its execution state and schedule its resumption.
      switch (status) {
        case TaskStatus.Completed<Output> s -> {
          // Propagate completion up the span hierarchy.
          // TERMINATION: The span hierarchy is a finite tree, so eventually we find a parentless span.
          var span = scheduler.span;
          while (true) {
            if (this.spanContributorCount.get(span).decrementAndGet() > 0) break;
            this.spanContributorCount.remove(span);

           this.spans.compute(span, (_id, $) -> $.close(currentTime.duration()));

            final var span$ = this.spans.get(span).parent;
            if (span$.isEmpty()) break;

            span = span$.get();
          }

          // Notify any blocked caller of our completion.
          progress.caller().ifPresent($ -> {
            if (this.blockedTasks.get($).decrementAndGet() == 0) {
              this.blockedTasks.remove($);
              this.scheduledJobs.schedule(JobId.forTask($), SubInstant.Tasks.at(currentTime.duration()));
            }
          });
        }
        case TaskStatus.Delayed<Output> s -> {
          if (s.delay().isNegative()) throw new IllegalArgumentException("Cannot schedule a task in the past");
          this.tasks.put(task, progress.continueWith(scheduler.span, scheduler.shadowedSpans, s.continuation()));
          this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime.duration().plus(s.delay())));
        }
        case TaskStatus.CallingTask<Output> s -> {
          final var target = TaskId.generate();
          SimulationEngine.this.spanContributorCount.get(scheduler.span).increment();
          SimulationEngine.this.tasks.put(target, new ExecutionState<>(scheduler.span, 0, Optional.of(task), s.child().create(this.executor), currentTime.duration()));
          SimulationEngine.this.blockedTasks.put(task, new MutableInt(1));
          frame.signal(JobId.forTask(target));

          this.tasks.put(task, progress.continueWith(scheduler.span, scheduler.shadowedSpans, s.continuation()));
        }
        case TaskStatus.AwaitingCondition<Output> s -> {
          final var condition = ConditionId.generate(task);
          this.conditions.put(condition, s.condition());
          this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(currentTime.duration()));

          this.tasks.put(task, progress.continueWith(scheduler.span, scheduler.shadowedSpans, s.continuation()));
          this.waitingTasks.put(condition, task);
        }
      }
  }

  /** Determine when a condition is next true, and schedule a signal to be raised at that time. */
  public void updateCondition(
      final ConditionId condition,
      final TaskFrame<JobId> frame,
      final SubInstantDuration currentTime,
      final Duration horizonTime,
      final Topic<Topic<?>> queryTopic) {
    if (trace) System.out.println("updateCondition(ConditionId=" + condition + ", queryTopic=" + queryTopic + ")");
    final var querier = new EngineQuerier(currentTime, frame, queryTopic, condition.sourceTask(), null);
    final var prediction = this.conditions
        .get(condition)
        .nextSatisfied(querier, Duration.MAX_VALUE) //horizonTime.minus(currentTime)
        .map(currentTime.duration()::plus);

    if (trace) System.out.println("updateCondition(): prediction = " + prediction);

    if (trace) System.out.println("updateCondition(): waitingConditions.subscribeQuery(conditionId=" + condition + ", querier.referencedTopics=" + querier.referencedTopics + ")");
    this.waitingConditions.subscribeQuery(condition, querier.referencedTopics);

    final Optional<Duration> expiry = querier.expiry.map(d -> currentTime.duration().plus((Duration)d));
    if (trace) System.out.println("updateCondition(): expiry = " + expiry);
    if (prediction.isPresent() && (expiry.isEmpty() || prediction.get().shorterThan(expiry.get()))) {
      var sjid = JobId.forSignal(condition);
      var t = SubInstant.Tasks.at(prediction.get());
      if (trace) System.out.println("updateCondition(): schedule(SignalJobId " + sjid + " at time " + t + ")");
      this.scheduledJobs.schedule(sjid, t);
    } else {
      // Try checking again later -- where "later" is in some non-zero amount of time!
      final var nextCheckTime = Duration.max(expiry.orElse(Duration.MAX_VALUE), currentTime.duration().plus(Duration.EPSILON));
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
      final SubInstantDuration currentTime
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
      var latestTime = ebt.floorKey(currentTime.duration());
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
          final List<TemporalEventSource.TimePoint.Commit> commits = timeline.commitsByTime.get(currentTime.duration());
          var topicsRemoved = timeline.topicsOfRemovedEvents.get(currentTime.duration());
          skipResourceEvaluation =
              topicsRemoved != null &&
              referencedTopics.stream().allMatch(t -> !timeline.isTopicStale(t, currentTime) ||
                                            (commits.stream().noneMatch(c -> c.topics().contains(t)) &&  // assumes replaced EventGraphs in current timeline
                                             topicsRemoved.contains(t)));
          if (skipResourceEvaluation) {
            this.timeline.removedResourceSegments.computeIfAbsent(currentTime.duration(), $ -> new HashSet<>()).add(resource.id());
          }
          if (debug) System.out.println("check for removed effects for resource " + resource.id() + " at " + currentTime.duration() + "; skipResourceEvaluation = " + skipResourceEvaluation);
        }

      }
    }

    final var querier = new EngineQuerier(currentTime, frame);
    if (!skipResourceEvaluation) {
      var profiles = this.resources.get(resource);
      // TODO: Should we check if the profile state hasn't been changing and if so not record them?
      //       if (profileIsChanging)
      {
        profiles.append(currentTime.duration(), querier);
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

    final Optional<Duration> expiry = querier.expiry.map(d -> currentTime.duration().plus((Duration)d));
    if (expiry.isPresent()) {
      this.scheduledJobs.schedule(JobId.forResource(resource), SubInstant.Resources.at(expiry.get()));
    }
  }

  /** Resets all tasks (freeing any held resources). The engine should not be used after being closed. */
  @Override
  public void close() {
    for (final var task : this.tasks.values()) {
      task.state().release();
    }

    this.executor.shutdownNow();
  }

  public MissionModel<?> getMissionModel() {
    return this.missionModel;
  }

  public SubInstantDuration curTime() {
    if (timeline == null) {
      return SubInstantDuration.ZERO;
    }
    return timeline.curTime();
  }

  public void setCurTime(Duration time) {
    if (!time.isEqualTo(curTime().duration())) {
      setCurTime(new SubInstantDuration(time, 0));
    }
  }

  public void setCurTime(SubInstantDuration time) {
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

  public record SpanInfo(
      Map<SpanId, ActivityDirectiveId> spanToPlannedDirective,
      Map<ActivityDirectiveId, SpanId> directiveIdToSpanId,
      Map<SpanId, SerializedActivity> input,
      Map<SpanId, SerializedValue> output,
      SimulationEngine engine
  ) {
    public SpanInfo(SimulationEngine engine) {
      this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), engine);
    }

    public boolean isActivity(final SpanId id) {
      return this.input.containsKey(id);
    }

    public SpanId getSpanIdForDirectiveId(ActivityDirectiveId id) {
      return directiveIdToSpanId.get(id);
    }

    public void removeSpan(final SpanId id) {
      var directiveId = spanToPlannedDirective.remove(id.id());
      if (directiveId != null) directiveIdToSpanId.remove(directiveId);
      input.remove(id.id());
      output.remove(id.id());
    }

    public boolean isDirective(SpanId id) {
      return this.spanToPlannedDirective.containsKey(id);
    }

    public ActivityDirectiveId getDirective(SpanId id) {
      return this.spanToPlannedDirective.get(id);
    }

    public record Trait(Map<Topic<?>, SerializableTopic<?>> topics, Topic<ActivityDirectiveId> activityTopic) implements EffectTrait<Consumer<SpanInfo>> {
      @Override
      public Consumer<SpanInfo> empty() {
        return spanInfo -> {};
      }

      @Override
      public Consumer<SpanInfo> sequentially(final Consumer<SpanInfo> prefix, final Consumer<SpanInfo> suffix) {
        return spanInfo -> { prefix.accept(spanInfo); suffix.accept(spanInfo); };
      }

      @Override
      public Consumer<SpanInfo> concurrently(final Consumer<SpanInfo> left, final Consumer<SpanInfo> right) {
        // SAFETY: `left` and `right` should commute. HOWEVER, if a span happens to directly contain two activities
        //   -- that is, two activities both contribute events under the same span's provenance -- then this
        //   does not actually commute.
        //   Arguably, this is a model-specific analysis anyway, since we're looking for specific events
        //   and inferring model structure from them, and at this time we're only working with models
        //   for which every activity has a span to itself.
        return spanInfo -> { left.accept(spanInfo); right.accept(spanInfo); };
      }

      public Consumer<SpanInfo> atom(final Event ev) {
        return spanInfo -> {
          // Identify activities.
          ev.extract(this.activityTopic)
            .ifPresent(directiveId -> {
              spanInfo.spanToPlannedDirective.put(spanInfo.engine.getSpanId(ev.provenance()), directiveId);
              spanInfo.directiveIdToSpanId.put(directiveId, spanInfo.engine.getSpanId(ev.provenance()));
            });

          for (final var topic : this.topics.values()) {
            // Identify activity inputs.
            extractInput(topic, ev, spanInfo);

            // Identify activity outputs.
            extractOutput(topic, ev, spanInfo);
          }
        };
      }

      private static <T>
      void extractInput(final SerializableTopic<T> topic, final Event ev, final SpanInfo spanInfo) {
        if (!topic.name().startsWith("ActivityType.Input.")) return;

        ev.extract(topic.topic()).ifPresent(input -> {
          final var activityType = topic.name().substring("ActivityType.Input.".length());

          spanInfo.input.put(
              spanInfo.engine.getSpanId(ev.provenance()),
              new SerializedActivity(activityType, topic.outputType().serialize(input).asMap().orElseThrow()));
        });
      }

      private static <T>
      void extractOutput(final SerializableTopic<T> topic, final Event ev, final SpanInfo spanInfo) {
        if (!topic.name().startsWith("ActivityType.Output.")) return;

        ev.extract(topic.topic()).ifPresent(output -> {
          spanInfo.output.put(
              spanInfo.engine.getSpanId(ev.provenance()),
              topic.outputType().serialize(output));
        });
      }
    }
  }

  private SpanInfo.Trait spanInfoTrait = null;
  public void updateTaskInfo(EventGraph<Event> g) {
    if (true) return;
    if (spanInfoTrait == null) spanInfoTrait = new SpanInfo.Trait(getMissionModel().getTopics(), defaultActivityTopic);
    g.evaluate(spanInfoTrait, spanInfoTrait::atom).accept(spanInfo);
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

    // Identify the nearest ancestor *activity* (excluding intermediate anonymous tasks).
    activityParents = new HashMap<SpanId, SpanId>();
    activityDirectiveIds = new HashMap<SpanId, ActivityDirectiveId>();
    this.spans.forEach((span, state) -> {
      if (!spanInfo.isActivity(span)) return;

      var parent = state.parent();
      while (parent.isPresent() && !spanInfo.isActivity(parent.get()) && !spanInfo.isDirective(parent.get())) {
        parent = this.spans.get(parent.get()).parent();
      }

      if (parent.isPresent()) {
        if (spanInfo.isActivity(parent.get())) {
          activityParents.put(span, parent.get());
        } else if (spanInfo.isDirective(parent.get())) {
          activityDirectiveIds.put(span, spanInfo.getDirective(parent.get()));
        }
      }
    });

    activityChildren = new HashMap<SpanId, List<SpanId>>();
    activityParents.forEach((activity, parent) -> {
      activityChildren.computeIfAbsent(parent, $ -> new LinkedList<>()).add(activity);
    });

    // Give every task corresponding to a child activity an ID that doesn't conflict with any root activity.
    spanToSimulatedActivityId = new HashMap<SpanId, SimulatedActivityId>(activityDirectiveIds.size());
    final var usedSimulatedActivityIds = new HashSet<>();
    for (final var entry : activityDirectiveIds.entrySet()) {
      var simActId = new SimulatedActivityId(entry.getValue().id());
      spanToSimulatedActivityId.put(entry.getKey(), simActId);
      directiveToSimulatedActivityId.put(entry.getValue(), simActId);
      usedSimulatedActivityIds.add(entry.getValue().id());
    }
    long counter = 1L;
    for (final var span : this.spans.keySet()) {
      if (!spanInfo.isActivity(span)) continue;

      if (spanToSimulatedActivityId.containsKey(span)) continue;

      while (usedSimulatedActivityIds.contains(counter)) counter++;
      spanToSimulatedActivityId.put(span, new SimulatedActivityId(counter++));
    }

//    final var simulatedActivities = new HashMap<SimulatedActivityId, SimulatedActivity>();
//    final var unfinishedActivities = new HashMap<SimulatedActivityId, UnfinishedActivity>();
    this.spans.forEach((span, state) -> {
      if (!spanInfo.isActivity(span)) return;

      final var activityId = spanToSimulatedActivityId.get(span);
      final var directiveId = activityDirectiveIds.get(span);

      if (state.endOffset().isPresent()) {
        final var inputAttributes = spanInfo.input().get(span);
        final var outputAttributes = spanInfo.output().get(span);

        this.simulatedActivities.put(activityId, new SimulatedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(state.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            state.endOffset().get().minus(state.startOffset()),
            spanToSimulatedActivityId.get(activityParents.get(span)),
            activityChildren.getOrDefault(span, Collections.emptyList()).stream().map(spanToSimulatedActivityId::get).toList(),
            (activityParents.containsKey(span)) ? Optional.empty() : Optional.of(directiveId),
            outputAttributes
        ));
      } else {
        final var inputAttributes = spanInfo.input().get(span);
        unfinishedActivities.put(activityId, new UnfinishedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(state.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            spanToSimulatedActivityId.get(activityParents.get(span)),
            activityChildren.getOrDefault(span, Collections.emptyList()).stream().map(spanToSimulatedActivityId::get).toList(),
            (activityParents.containsKey(span)) ? Optional.empty() : Optional.of(directiveId)
        ));
      }
    });

    final var serializableTopicToId = new HashMap<SerializableTopic<?>, Integer>();
    for (final var serializableTopic : serializableTopics.values()) {
      serializableTopicToId.put(serializableTopic, this.topics.size());
      this.topics.add(Triple.of(this.topics.size(), serializableTopic.name(), serializableTopic.outputType().getSchema()));
    }

    // Serialize the timeline of EventGraphs
    long totalCommits = 0;
    long totalGraphs = 0;
    long totalNonEmptyGraphs = 0;
    if (debug) System.out.println(timeline.commitsByTime.size() + " timepoints with commits");
    for (Duration time: timeline.commitsByTime.keySet()) {
      var commitList = timeline.commitsByTime.get(time);
      if (debug) totalCommits += commitList.size();
      for (var commit : commitList) {
        var events = timeline.withoutReadEvents(commit.events());
        if (debug) {
          long c = events.count();
          long ne = events.countNonEmpty();
          totalGraphs += c;
          totalNonEmptyGraphs += ne;
        }
        final var serializedEventGraph = events.substitute(
            event -> {
              EventGraph<Pair<Integer, SerializedValue>> output = EventGraph.empty();
              for (final var serializableTopic : serializableTopics.values()) {
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
    if (debug) System.out.println("TOTAL commits = " + totalCommits);
    if (debug) System.out.println("TOTAL graphs = " + totalGraphs);
    if (debug) System.out.println("TOTAL non-empty graphs = " + totalNonEmptyGraphs);
    if (debug) System.out.println("TOTAL empty graphs = " + (totalGraphs - totalNonEmptyGraphs));

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

  public Span getSpan(SpanId spanId) {
    return this.spans.get(spanId);
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
    private final SubInstantDuration currentTime;
    public final TaskFrame<Job> frame;
    public final Set<Topic<?>> referencedTopics = new HashSet<>();
    private final Optional<Triple<Topic<Topic<?>>, TaskId, SpanId>> queryTrackingInfo;
    public Optional<Duration> expiry = Optional.empty();

    public EngineQuerier(final SubInstantDuration currentTime, final TaskFrame<Job> frame, final Topic<Topic<?>> queryTopic,
                         final TaskId associatedActivity, final SpanId associatedSpan) {
      this.currentTime = currentTime;
      this.frame = Objects.requireNonNull(frame);
      this.queryTrackingInfo = Optional.of(Triple.of(Objects.requireNonNull(queryTopic), associatedActivity, associatedSpan));
    }

    public EngineQuerier(final SubInstantDuration currentTime, final TaskFrame<Job> frame) {
      this.currentTime = currentTime;
      this.frame = Objects.requireNonNull(frame);
      this.queryTrackingInfo = Optional.empty();
    }

    @Override
    public <State> State getState(final CellId<State> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via MissionModelBuilder).
      @SuppressWarnings("unchecked")
      final var query = ((EngineCellId<?, State>) token);

      this.expiry = min(this.expiry, this.frame.getExpiry(query.query()));
      this.referencedTopics.add(query.topic());

      // TODO: Cache the state (until the query returns) to avoid unnecessary copies
      //  if the same state is requested multiple times in a row.
      final var state$ = this.frame.getState(query.query());

      this.queryTrackingInfo.ifPresent(info -> {
        if (isTaskStale(info.getMiddle(), currentTime)) {
          final SubInstantDuration t = staleTasks.get(info.getMiddle());
          var causalIndex = this.frame.tip.points.length;
          var staleIndex = staleCausalEventIndex.get(info.getMiddle());
          // Create a noop event to mark when the read occurred in the EventGraph
          var noop = Event.create(info.getLeft(), query.topic(), info.getMiddle());
          this.frame.emit(noop);
          putInCellReadHistory(query.topic(), info.getMiddle(), noop, currentTime);
        }
      });

      return state$.orElseThrow(IllegalArgumentException::new);
    }

    private static Optional<Duration> min(final Optional<Duration> a, final Optional<Duration> b) {
      if (a.isEmpty()) return b;
      if (b.isEmpty()) return a;
      return Optional.of(Duration.min(a.get(), b.get()));
    }
  }

  /** A handle for processing requests and effects from a modeled task. */
  private final class EngineScheduler implements Scheduler {
    private final SubInstantDuration currentTime;
    private int shadowedSpans;
    private TaskId activeTask;
    private SpanId span;
    private final Optional<TaskId> caller;
    private final TaskFrame<JobId> frame;
    private final Topic<Topic<?>> queryTopic;

    public EngineScheduler(
        final SubInstantDuration currentTime,
        final int shadowedSpans,
        final TaskId activeTask,
        final SpanId span,
        final Optional<TaskId> caller,
        final TaskFrame<JobId> frame,
        final Topic<Topic<?>> queryTopic)
    {
      this.currentTime = Objects.requireNonNull(currentTime);
      this.shadowedSpans = shadowedSpans;
      this.activeTask = activeTask;
      this.span = Objects.requireNonNull(span);
      this.caller = Objects.requireNonNull(caller);
      this.frame = Objects.requireNonNull(frame);
      this.queryTopic = Objects.requireNonNull(queryTopic);
    }

    @Override
    public <State> State get(final CellId<State> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via MissionModelBuilder).
      @SuppressWarnings("unchecked")
      final var query = (EngineCellId<?, State>) token;

      // find or create a cell for the query and step it up -- this used to be done in LiveCell.get()
      final var state$ = this.frame.getState(query.query());

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
      //       if the same state is requested multiple times in a row.
      return state$.orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public <EventType> void emit(final EventType event, final Topic<EventType> topic) {
      if (debug) System.out.println("emit(" + event + ", " + topic + ")");
      if (debug) System.out.println("emit(): isTaskStale() --> " + isTaskStale(this.activeTask, this.currentTime));
      if (isTaskStale(this.activeTask, this.currentTime)) {
        // Append this event to the timeline.
        this.frame.emit(Event.create(topic, event, this.activeTask));
        if (debug) System.out.println("emit(): isTopicStale(" + topic + ") --> " + timeline.isTopicStale(topic, this.currentTime));
        if (!timeline.isTopicStale(topic, this.currentTime)) {
          SimulationEngine.this.timeline.setTopicStale(topic, this.currentTime);
        }
        SimulationEngine.this.invalidateTopic(topic, this.currentTime.duration());
      }
    }

    @Override
    public <ActDirectiveId> void startDirective(
        final ActDirectiveId activityDirectiveId,
        final Topic<ActDirectiveId> activityTopic)
    {
      if (activityDirectiveId instanceof ActivityDirectiveId aId) {
        SimulationEngine.this.startDirective(aId, (Topic<ActivityDirectiveId>)activityTopic, this.span);
      }
    }

    @Override
    public <T> void startActivity(final T activity, final Topic<T> inputTopic) {
      SimulationEngine.this.startActivity(activity, inputTopic, this.span);
    }

    @Override
    public <T> void endActivity(final T result, final Topic<T> outputTopic) {
      SimulationEngine.this.endActivity(result, outputTopic, this.span);
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
            var lastEventTimePlusE = eventMap == null ? null : new SubInstantDuration(eventMap.lastKey(), eventMap.lastEntry().getValue().size() + 1);
            if (lastEventTimePlusE != null) {
              setTaskStale(task, lastEventTimePlusE, null);
            }
          }
        }
        // Record task information
        SimulationEngine.this.spanContributorCount.get(this.span).increment();
        SimulationEngine.this.tasks.put(task, new ExecutionState<>(this.span, 0, this.caller,
                                                                   state.create(SimulationEngine.this.executor),
                                                                   currentTime.duration()));
        this.caller.ifPresent($ -> SimulationEngine.this.blockedTasks.get($).increment());
        SimulationEngine.this.taskFactories.put(task, state);
        SimulationEngine.this.taskIdsForFactories.put(state, task);
        this.frame.signal(JobId.forTask(task));
      }
    }

    @Override
    public void pushSpan() {
      final var parentSpan = this.span;
      this.shadowedSpans += 1;
      this.span = SpanId.generate();

      SimulationEngine.this.spans.put(this.span, new Span(Optional.of(parentSpan), this.currentTime.duration(), Optional.empty()));
      SimulationEngine.this.spanContributorCount.put(this.span, new MutableInt(1));
    }

    @Override
    public void popSpan() {
      // TODO: Do we want to throw an error instead?
      if (this.shadowedSpans == 0) return;
      final SpanId parentSpan = SimulationEngine.this.spans.get(this.span).parent().orElseThrow();

      if (SimulationEngine.this.spanContributorCount.get(this.span).decrementAndGet() == 0) {
        SimulationEngine.this.spanContributorCount.remove(this.span);
        SimulationEngine.this.spans.compute(this.span, (_id, $) -> $.close(currentTime.duration()));
        // Parent span contributor count remains constant, because this.span is removed, and this task is added
      } else {
        // Parent span contributor count increases by one, because this task is added without removing this.span
        SimulationEngine.this.spanContributorCount.get(parentSpan).increment();
      }

      // NOTE: We don't need to propagate completion any further, because the next shadowed span
      // has by definition not been completed: this task may still contribute to it, and this task
      // has not terminated.

      this.shadowedSpans -= 1;
      this.span = parentSpan;
    }
  }

  private void startDirective(ActivityDirectiveId directiveId, Topic<ActivityDirectiveId> activityTopic, SpanId activeSpan) {
    spanInfo.spanToPlannedDirective.put(activeSpan, directiveId);
    spanInfo.directiveIdToSpanId.put(directiveId, activeSpan);
  }

  private <T> void startActivity(T activity, Topic<T> inputTopic, final SpanId activeSpan) {
    final SerializableTopic<T> sTopic = (SerializableTopic<T>) getMissionModel().getTopics().get(inputTopic);
    if (sTopic == null) return; // ignoring unregistered activity types!
    final var activityType = sTopic.name().substring("ActivityType.Input.".length());

    spanInfo.input.put(
        activeSpan,
        new SerializedActivity(activityType, sTopic.outputType().serialize(activity).asMap().orElseThrow()));
  }

  private <T> void endActivity(T result, Topic<T> outputTopic, SpanId activeSpan) {
    final SerializableTopic<T> sTopic = (SerializableTopic<T>) getMissionModel().getTopics().get(outputTopic);
    if (sTopic == null) return; // ignoring unregistered activity types!
    spanInfo.output.put(
        activeSpan,
        sTopic.outputType().serialize(result));
  }

  public static <E, T>
  TaskFactory<T> emitAndThen(final E event, final Topic<E> topic, final TaskFactory<T> continuation) {
    return executor -> scheduler -> {
      scheduler.emit(event, topic);
      return continuation.create(executor).step(scheduler);
    };
  }

  private boolean isActivity(final TaskId taskId) {
    SpanId spanId = getSpanId(taskId);
    if (spanId != null && this.spanInfo.isActivity(spanId)) return true;
    if (this.daemonTasks.contains(taskId)) return false;
    if (oldEngine == null) return false;
    return this.oldEngine.isActivity(taskId);
  }

  private TaskId getTaskParent(TaskId taskId) {
    var spanId = getSpanId(taskId);
    TaskId parent = null;
    if (spanId != null && activityParents != null && !activityParents.isEmpty()) {
      var parentSpanId = activityParents.get(spanId);
      if (parentSpanId != null) {
        var tasks = getTaskIds(spanId);
        if (tasks != null && !tasks.isEmpty()) {
          parent = tasks.getFirst();
        }
      }
    }
    if (parent == null && oldEngine != null) {
      parent = oldEngine.getTaskParent(taskId);
    }
    return parent;
  }

  boolean isDaemonTask(TaskId taskId) {
    if (daemonTasks.contains(taskId)) return true;
    SpanId spanId = getSpanId(taskId);
    if (spanId != null && spanInfo.isActivity(spanId)) return false;
    if (oldEngine != null) {
      return oldEngine.isDaemonTask(taskId);
    }
    return false;
  }

  public ActivityDirectiveId getActivityDirectiveId(TaskId taskId) {
    var spanId = getSpanId(taskId);
    var activityDirectiveId = spanId == null ? null : spanInfo.spanToPlannedDirective.get(spanId);
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
    var spanId = getSpanId(taskId);
    SerializedActivity serializedActivity = spanId == null ? null : this.spanInfo.input.get(spanId);
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
    var spanId = getSpanId(taskId);
    Set<TaskId> taskChildren = null;
    if (spanId != null && activityChildren != null && !activityChildren.isEmpty()) {
      var childSpans = activityChildren.get(spanId);
      if (childSpans != null) {
        final Set<TaskId> children = new HashSet<>();
        childSpans.forEach(s -> {
          var tasks = getTaskIds(s);
          if (tasks != null) children.addAll(tasks);
        });
        taskChildren = children;
      }
    }
    if (oldEngine != null && (taskChildren == null || taskChildren.isEmpty())) {
      taskChildren = oldEngine.getTaskChildren(taskId);
    }
    if (taskChildren == null) taskChildren = Collections.emptySet();
    return taskChildren;
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
      SerializedActivity serializedActivity = this.spanInfo.input.get(taskId.id());
      var activityDirectiveId = spanInfo.spanToPlannedDirective.get(taskId.id());
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

    /** A job to resume a task blocked on a condition. */
    record SignalJobId(ConditionId id) implements JobId {}

    /** A job to query a resource. */
    record ResourceJobId(ResourceId id) implements JobId {}

    /** A job to check a condition. */
    record ConditionJobId(ConditionId id) implements JobId {}

    static TaskJobId forTask(final TaskId task) {
      return new TaskJobId(task);
    }

    static SignalJobId forSignal(final ConditionId signal) {
      return new SignalJobId(signal);
    }

    static ResourceJobId forResource(final ResourceId resource) {
      return new ResourceJobId(resource);
    }

    static ConditionJobId forCondition(final ConditionId condition) {
      return new ConditionJobId(condition);
    }
  }

  /** The state of an executing task. */
  private record ExecutionState<Output>(SpanId span, int shadowedSpans, Optional<TaskId> caller, Task<Output> state, Duration startOffset) {
    public ExecutionState<Output> continueWith(final SpanId span, final int shadowedSpans, final Task<Output> newState) {
      return new ExecutionState<>(span, shadowedSpans, this.caller, newState, startOffset());
    }
  }

  /** The span of time over which a subtree of tasks has acted. */
  public record Span(Optional<SpanId> parent, Duration startOffset, Optional<Duration> endOffset) {
    /** Close out a span, marking it as inactive past the given time. */
    public Span close(final Duration endOffset) {
      if (this.endOffset.isPresent()) throw new Error("Attempt to close an already-closed span");
      return new Span(this.parent, this.startOffset, Optional.of(endOffset));
    }

    public Optional<Duration> duration() {
      return this.endOffset.map($ -> $.minus(this.startOffset));
    }

    public boolean isComplete() {
      return this.endOffset.isPresent();
    }
  }

//  public static Duration getEndOffset(ExecutionState<?> s) {
//    if (s instanceof ExecutionState.InProgress<?> ip) {
//      ip.
//      return null;
//    }
//  }
}
