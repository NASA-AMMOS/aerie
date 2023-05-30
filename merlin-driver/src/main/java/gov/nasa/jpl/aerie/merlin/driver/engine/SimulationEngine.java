package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.CombinedSimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel.SerializableTopic;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A representation of the work remaining to do during a simulation, and its accumulated results.
 */
public final class SimulationEngine implements AutoCloseable {
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

  /** The history of when tasks read topics/cells */
  private final HashMap<Topic<?>, TreeMap<Duration, HashMap<TaskId, Event>>> cellReadHistory = new HashMap<>();

  private final MissionModel<?> missionModel;

  /** The start time of the simulation, from which other times are offsets */
  private final Instant startTime;

  public Map<ActivityDirectiveId, ActivityDirective> scheduledDirectives = null;
  public Map<String, Map<ActivityDirectiveId, ActivityDirective>> directivesDiff = null;

  public final TaskInfo taskInfo = new TaskInfo();
//  private Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> realProfiles = new HashMap<>();
//  private Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> discreteProfiles = new HashMap<>();
  private final HashMap<SimulatedActivityId, SimulatedActivity> simulatedActivities = new HashMap<>();
  private final HashMap<SimulatedActivityId, UnfinishedActivity> unfinishedActivities = new HashMap<>();
  private final SortedMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> serializedTimeline = new TreeMap<>();
  private final List<Triple<Integer, String, ValueSchema>> topics = new ArrayList<>();
  private SimulationResults simulationResults = null;
  public static final Topic<ActivityDirectiveId> defaultActivityTopic = new Topic<>();

  public SimulationEngine(Instant startTime, MissionModel<?> missionModel, SimulationEngine oldEngine) {

    this.startTime = startTime;
    this.missionModel = missionModel;
    this.oldEngine = oldEngine;
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

//<<<<<<< HEAD
  /**  */
  public void putInCellReadHistory(Topic<?> topic, TaskId taskId, Event noop, Duration time) {
    // TODO: Can't we just get this from eventsByTopic instead of having a separate data structure?
    var inner = cellReadHistory.computeIfAbsent(topic, $ -> new TreeMap<>());
    inner.computeIfAbsent(time, $ -> new HashMap<>()).put(taskId, noop);
  }

  /**
   * Get the earliest time within a specified range that potentially stale cells are read by tasks not scheduled
   * to be re-run.
   * @param after start of time range
   * @param before end of time range
   * @return the time of the earliest read, the tasks doing the reads, and the noop Events/Topics read by each task
   */
  public Pair<Duration, Map<TaskId, HashSet<Pair<Topic<?>, Event>>>> earliestStaleReads(Duration after, Duration before) {
    // We need to have the reads sorted according to the event graph.  Currently, this function doesn't
    // handle a task reading a cell more than once in a graph.  But, we should make sure we handle this case. TODO
    var earliest = Duration.MAX_VALUE;
    final var tasks = new HashMap<TaskId, HashSet<Pair<Topic<?>, Event>>>();
    final var topicsStale = timeline.staleTopics.keySet();
    for (var topic : topicsStale) {
      var topicReads = cellReadHistory.get(topic);
      if (topicReads == null || topicReads.isEmpty()) {
        continue;
      }
      final NavigableMap<Duration, HashMap<TaskId, Event>> topicReadsAfter =
          topicReads.subMap(after, false, Duration.min(earliest, before), true);
      if (topicReadsAfter == null || topicReadsAfter.isEmpty()) {
        continue;
      }
      for (var entry : topicReadsAfter.entrySet()) {
        Duration d = entry.getKey();
        HashMap<TaskId, Event> taskIds = entry.getValue();
        if (timeline.isTopicStale(topic, d)) {
          if (d.shorterThan(earliest)) {
            earliest = d;
            tasks.clear();
          } else if (d.longerThan(earliest)) {
            continue;
          }
          taskIds.forEach((id, event) -> tasks.computeIfAbsent(id, $ -> new HashSet<>()).add(Pair.of(topic, event)));
        }
      }
    }
    return Pair.of(earliest, tasks);
  }

  /** Get the earliest time that topics become stale and return those topics with the time */
  public Pair<List<Topic<?>>, Duration> earliestStaleTopics(Duration before) {
    var list = new ArrayList<Topic<?>>();
    Duration earliest = Duration.MAX_VALUE;
    for (var entry : timeline.staleTopics.entrySet()) {
      Duration d = null;
      for (var e : entry.getValue().entrySet()) {
        if (e.getValue()) {
          d = e.getKey();
          break;
        }
      }
      if (d == null || d.noShorterThan(before)) {
        continue;
      }
      int comp = d.compareTo(earliest);
      if (comp <= 0) {
        if (comp < 0) list = new ArrayList<>();
        list.add(entry.getKey());
        earliest = d;
      }
    }
    return Pair.of(list, earliest);
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
        return;
      }
    }
    staleTasks.put(taskId, time);
    var execState = oldEngine.tasks.get(taskId);
    final Duration taskStart;
    if (execState != null) taskStart = execState.startOffset();
    else {
      taskStart = Duration.ZERO;
      throw new RuntimeException("Can't find task start!");
    }
    rescheduleTask(taskId, taskStart);
    removeTaskHistory(taskId);
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
        List<TemporalEventSource.TimePoint.Commit> events = this.timeline.commitsByTime.get(timeOfStaleReads);
        if (events == null || events.isEmpty()) throw new RuntimeException("No EventGraph for potentially stale read.");
        this.timeline.stepUp(tempCell, events.get(events.size()-1).events(), noop, false);
        // Assumes that the same noop event for the read exists at the same time in the oldTemporalEventSource.
        var oldEvents = this.timeline.oldTemporalEventSource.commitsByTime.get(timeOfStaleReads);
        if (oldEvents == null || oldEvents.isEmpty()) throw new RuntimeException("No old EventGraph for potentially stale read.");
        if (timeline.isTopicStale(topic, timeOfStaleReads) || !oldEvents.equals(events)) {
          // Assumes the old cell has been stepped up to the same time already.  TODO: But, if not stale, shouldn't the old cell not exist or not be stepped up, in which case we duplicate to get the old cell instead unless the old event graph is the same?
          var tempOldCell = timeline.getOldCell(steppedCell).map(Cell::duplicate);
          this.timeline.oldTemporalEventSource.stepUp(tempOldCell.orElseThrow(),
                                                      oldEvents.get(oldEvents.size()-1).events(), noop, false);
          if (!tempCell.getState().equals(tempOldCell.get().getState())) {
            // Mark stale and reschedule task
            setTaskStale(taskId, timeOfStaleReads);
            break;  // rescheduled task, so can move on to the next task
          }
        }
      }  // for Pair<Topic<?>, Event>
    }  // for Map.Entry<TaskId, HashSet<Pair<Topic<?>, Event>>>
  }

  public void removeTaskHistory(TaskId taskId) {
    // TODO:  cellReadHistory
    // Look for the task's Events in the old and new timelines.
    final TreeMap<Duration, List<EventGraph<Event>>> graphsForTask = this.timeline.eventsByTask.get(taskId);
    final TreeMap<Duration, List<EventGraph<Event>>> oldGraphsForTask = this.oldEngine.timeline.eventsByTask.get(taskId);
    var allKeys = new TreeSet<Duration>();
    if (graphsForTask != null) {
      allKeys.addAll(graphsForTask.keySet());
    }
    if (oldGraphsForTask != null) {
      allKeys.addAll(oldGraphsForTask.keySet());
    }
    for (Duration time : allKeys) {
      List<EventGraph<Event>> gl = graphsForTask == null ? null : graphsForTask.get(time); // If old graph is already replaced used the replacement
      if (gl == null || gl.isEmpty()) gl = oldGraphsForTask == null ? null : oldGraphsForTask.get(time);  // else we can replace the old graph
      for (var g : gl) {
//        // invalidate topics for cells affected by the task in the old graph so that resource values are checked at
//        // this time to erase effects on resources  -- TODO: this doesn't work!  only one scheduled job per resource
//        var s = new HashSet<Topic<?>>();
//        TemporalEventSource.extractTopics(s, g, e -> taskId.equals(e.provenance()));
//        s.forEach(topic -> invalidateTopic(topic, time));
        // replace the old graph with one without the task's events, updating data structures
        var newG = g.filter(e -> !taskId.equals(e.provenance()));
        if (newG != g) {
          timeline.replaceEventGraph(g, newG);
          updateTaskInfo(newG);
        }
      }
    }
    // remove task from taskInfo data structures
    taskInfo.removeTask(taskId);

    // Remove children, too!
    var children = this.taskChildren.get(taskId);
    if (children != null) children.forEach(c -> removeTaskHistory(c));
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
    final var resources = this.waitingResources.invalidateTopic(topic);
//    if (!resources.isEmpty()) {
//      System.out.println("invalidate topic: " + topic + " at " + invalidationTime + " and schedule jobs for " + resources.stream().map(r -> r.id()).toList());
//    }
    for (final var resource : resources) {
      this.scheduledJobs.schedule(JobId.forResource(resource), SubInstant.Resources.at(invalidationTime));
    }

    final var conditions = this.waitingConditions.invalidateTopic(topic);
    for (final var condition : conditions) {
      // If we were going to signal tasks on this condition, well, don't do that.
      // Schedule the condition to be rechecked ASAP.
      this.scheduledJobs.unschedule(JobId.forSignal(SignalId.forCondition(condition)));
      this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(invalidationTime));
    }
  }

//<<<<<<< HEAD
  /** Returns the offset time of the next batch of scheduled jobs. */
  public Duration timeOfNextJobs() {
    return this.scheduledJobs.timeOfNextJobs();
  }

//  /** Removes and returns the next set of jobs to be performed concurrently. */
//  public JobSchedule.Batch<JobId> extractNextJobs(final Duration maximumTime) {
//    final var batch = this.scheduledJobs.extractNextJobs(maximumTime);
//=======
  /** Performs a collection of tasks concurrently, extending the given timeline by their stateful effects. */
//  public void step() {
  public void step(final Duration maximumTime, final Topic<Topic<?>> queryTopic) {
//>>>>>>> prototype/excise-resources-from-sim-engine
    //System.out.println("step(): begin -- time = " + curTime());
    var timeOfNextJobs = timeOfNextJobs();
    var nextTime = timeOfNextJobs;

    var earliestStaleReads = earliestStaleReads(curTime(), nextTime);  // might want to not limit by nextTime and cache for future iterations
    var staleReadTime = earliestStaleReads.getLeft();
    nextTime = Duration.min(nextTime, staleReadTime);

    // Increment real time, if necessary.
    var timeForDelta = Duration.min(nextTime, maximumTime);
    final var delta = timeForDelta.minus(curTime());
    setCurTime(timeForDelta);
//          if (!delta.isNegative()) {
//            engine.timeline.add(delta);
//          }
    // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
    //   even if they occur at the same real time.

    if (nextTime.longerThan(maximumTime) || nextTime.isEqualTo(Duration.MAX_VALUE)) {
      //System.out.println("step(): end -- time elapsed (" + curTime() + ") past maximum (" + maximumTime + ")");
      return;
    }

    if (staleReadTime.isEqualTo(nextTime)) {
      rescheduleStaleTasks(earliestStaleReads);
    }

    if (timeOfNextJobs.isEqualTo(nextTime)) {

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

//    this.timeline.add(batch.offsetFromStart().minus(curTime()));
//    this.elapsedTime = batch.offsetFromStart();
      setCurTime(batch.offsetFromStart());
//<<<<<<< HEAD
//  /** Performs a collection of tasks concurrently, extending the given timeline by their stateful effects. */
//  public EventGraph<Event> performJobs(
//      final Collection<JobId> jobs,
//      final Duration currentTime,
//      final Duration maximumTime,
//      final Topic<Topic<?>> queryTopic) {
//    var tip = EventGraph.<Event>empty();
//    for (final var job$ : jobs) {
//      tip = EventGraph.concurrently(tip, TaskFrame.run(job$, this.cells, (job, frame) -> {
//        this.performJob(job, frame, currentTime, maximumTime, queryTopic);
//=======
      var tip = EventGraph.<Event>empty();
      for (final var job$ : batch.jobs()) {
        tip = EventGraph.concurrently(tip, TaskFrame.run(job$, this.cells, (job, frame) -> {
          this.performJob(job, frame, curTime(), maximumTime, queryTopic);
//        this.performJob(job, frame, batch.offsetFromStart());
//>>>>>>> prototype/excise-resources-from-sim-engine
        }));
      }

//    this.timeline.add(tip);
      this.timeline.add(tip, curTime());
      updateTaskInfo(tip);

      //System.out.println("step(): end -- time = " + curTime());
    }
  }

//  public Duration getElapsedTime() {
//    return this.elapsedTime;
//  }

  /** Performs a single job. */
  private void performJob(
      final JobId job,
      final TaskFrame<JobId> frame,
//<<<<<<< HEAD
      final Duration currentTime,
      final Duration maximumTime,
      final Topic<Topic<?>> queryTopic) {
//=======
//      final Duration currentTime
//  ) {
//>>>>>>> prototype/excise-resources-from-sim-engine
    if (job instanceof JobId.TaskJobId j) {
      this.stepTask(j.id(), frame, currentTime, queryTopic);
    } else if (job instanceof JobId.SignalJobId j) {
      this.stepSignalledTasks(j.id(), frame);
    } else if (job instanceof JobId.ConditionJobId j) {
//<<<<<<< HEAD
      this.updateCondition(j.id(), frame, currentTime, maximumTime, queryTopic);
//=======
//      this.updateCondition(j.id(), frame, currentTime);
//>>>>>>> prototype/excise-resources-from-sim-engine
    } else if (job instanceof JobId.ResourceJobId j) {
      // TODO: Would like to check if the cells on which this resource depends is stale.
      //       Where is this info?  EngineQuerier.referencedTopics?
      //       [Remove this comment when answered before merging changes.]
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
      this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(currentTime));

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
//<<<<<<< HEAD
      final Duration currentTime,
      final Duration horizonTime,
      final Topic<Topic<?>> queryTopic) {
    final var querier = new EngineQuerier(currentTime, frame, queryTopic, condition.sourceTask());
//=======
//      final Duration currentTime
//  ) {
//    final var querier = new EngineQuerier(frame);
//>>>>>>> prototype/excise-resources-from-sim-engine
    final var prediction = this.conditions
        .get(condition)
        .nextSatisfied(querier, Duration.MAX_VALUE)
        .map(currentTime::plus);

    this.waitingConditions.subscribeQuery(condition, querier.referencedTopics);

    final Optional<Duration> expiry = querier.expiry.map(d -> currentTime.plus((Duration)d));
    if (prediction.isPresent() && (expiry.isEmpty() || prediction.get().shorterThan(expiry.get()))) {
      this.scheduledJobs.schedule(JobId.forSignal(SignalId.forCondition(condition)), SubInstant.Tasks.at(prediction.get()));
    } else {
      // Try checking again later -- where "later" is in some non-zero amount of time!
      final var nextCheckTime = Duration.max(expiry.orElse(Duration.MAX_VALUE), currentTime.plus(Duration.EPSILON));
      this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(nextCheckTime));
    }
  }

  /** Get the current behavior of a given resource and accumulate it into the resource's profile. */
  public void updateResource(
      final ResourceId resource,
      final TaskFrame<JobId> frame,
      final Duration currentTime
  ) {
    // TODO -- this would be better with the ResourceTracker from the branch, prototype/excise-resources-from-sim-engine

    // We want to avoid saving profile segments if they aren't changing.  We also don't want to compute the resource if
    // none of the cells on which it depends are stale.
    boolean skipResourceEvaluation = false;
    if (oldEngine != null) {
      var ebt = oldEngine.timeline.commitsByTime;
      var latestTime = ebt.floorKey(currentTime);
      // If no events since plan start, then can't be stale, so nothing to do.
      if (latestTime == null) skipResourceEvaluation = true;
      else {
        // Note that there may or may not be events at this currentTime.
        // So, how can we know it is not stale?
        // - No cells are stale
        // - If the past resource value was not based on stale information and matched the previous simulation
        //   (henceforth, the resource is not stale), and if the resource's referencedTopics in waitingResources
        //   are not stale, hen the evaluation may be skipped.
        // - So, should we choose a different expiry? Probably not--just make this evaluation fast.
        //   And, with staleness, we can determine that we need not invalidate a topic in some cases.

        // Check if any of the resource's referenced topics are stale
        var topics = this.waitingResources.getTopics(resource);
        var resourceIsStale = topics.stream().anyMatch(t -> timeline.isTopicStale(t, currentTime));
        if (resourceIsStale) {
//          System.out.println("skipping evaluation of resource " + resource.id() + " at " + currentTime);
          skipResourceEvaluation = true;
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
        this.waitingResources.subscribeQuery(resource, querier.referencedTopics);
//        System.out.println("querier, " + querier + " subscribing " + resource.id() + " to referenced topics: " + querier.referencedTopics);
      }
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

//<<<<<<< HEAD
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

//  public record TaskInfo(
//=======
  public boolean hasJobsScheduledThrough(final Duration givenTime) {
    return this.scheduledJobs
        .min()
        .map($ -> $.project().noLongerThan(givenTime))
        .orElse(false);
  }

  public record TaskInfo(
//  private record TaskInfo(
//>>>>>>> prototype/excise-resources-from-sim-engine
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

//<<<<<<< HEAD
  private TaskInfo.Trait taskInfoTrait = null;
  public void updateTaskInfo(EventGraph<Event> g) {
    if (taskInfoTrait == null) taskInfoTrait = new TaskInfo.Trait(getMissionModel().getTopics(), defaultActivityTopic);
    g.evaluate(taskInfoTrait, taskInfoTrait::atom).accept(taskInfo);
  }
//=======
//  public static SimulationResults computeResults(
  public SimulationResultsInterface computeResults(
//      final SimulationEngine engine,
      final Instant startTime,
      final Duration elapsedTime,
      final Topic<ActivityDirectiveId> activityTopic
//      final TemporalEventSource timeline,
//      final Iterable<SerializableTopic<?>> serializableTopics
  )
  {
    return computeResults(
//        engine,
        startTime,
        elapsedTime,
        activityTopic,
//        timeline,
//        serializableTopics,
//        engine
        this
            .resources
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                        $ -> $.getKey().id(),
                        Map.Entry::getValue)));
//>>>>>>> prototype/excise-resources-from-sim-engine
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
//<<<<<<< HEAD
//      final Topic<ActivityDirectiveId> activityTopic
//=======
      final Topic<ActivityDirectiveId> activityTopic,
//      final TemporalEventSource timeline,
//      final Iterable<SerializableTopic<?>> serializableTopics,
      final Map<String, ProfilingState<?>> resources
//>>>>>>> prototype/excise-resources-from-sim-engine
  ) {
    //System.out.println("computeResults(startTime=" + startTime + ", elapsedTime=" + elapsedTime + "...) at time " + curTime());

//    // Collect per-task information from the event graph.
//    taskInfo = new TaskInfo();

    var serializableTopics = this.missionModel.getTopics();
//    for (final var point : timeline) {
//      if (!(point instanceof TemporalEventSource.TimePoint.Commit p)) continue;
//      updateTaskInfo(p.events());
//    }

    // Extract profiles for every resource.
    final var realProfiles = new HashMap<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>>();

//<<<<<<< HEAD
//    //var allResources = oldEngine == null ? this.resources : new HashMap<>(oldEngine.resources).putAll(this.resources);
    for (final var entry : resources.entrySet()) {
//      final var id = entry.getKey();
//=======
//    for (final var entry : resources.entrySet()) {
      final var name = entry.getKey();
//>>>>>>> prototype/excise-resources-from-sim-engine
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
    final var taskToSimulatedActivityId = new HashMap<String, SimulatedActivityId>(taskInfo.taskToPlannedDirective.size());
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

//    final var simulatedActivities = new HashMap<SimulatedActivityId, SimulatedActivity>();
//    final var unfinishedActivities = new HashMap<SimulatedActivityId, UnfinishedActivity>();
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
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.of(directiveId)
        ));
      } else if (state instanceof ExecutionState.AwaitingChildren<?> e){
        final var inputAttributes = this.taskInfo.input().get(task.id());
        this.unfinishedActivities.put(activityId, new UnfinishedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.of(directiveId)
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
    return new CombinedSimulationResults(this.simulationResults, oldEngine.getCombinedSimulationResults());
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
//<<<<<<< HEAD
//  private final class EngineQuerier implements Querier {
  public final class EngineQuerier<Job> implements Querier {
    private final Duration currentTime;
//    private final TaskFrame<JobId> frame;
    public final TaskFrame<Job> frame;
//    private final Set<Topic<?>> referencedTopics = new HashSet<>();
    public final Set<Topic<?>> referencedTopics = new HashSet<>();
    private final Optional<Pair<Topic<Topic<?>>, TaskId>> queryTrackingInfo;
    public Optional<Duration> expiry = Optional.empty();

    public EngineQuerier(final Duration currentTime, final TaskFrame<Job> frame, final Topic<Topic<?>> queryTopic,
                         final TaskId associatedTask) {
      this.currentTime = currentTime;
//=======
//  public static final class EngineQuerier implements Querier {
//    private final TaskFrame<?> frame;
//    public final Set<Topic<?>> referencedTopics = new HashSet<>();
//    public Optional<Duration> expiry = Optional.empty();
//
//    public EngineQuerier(final TaskFrame<?> frame) {
//>>>>>>> prototype/excise-resources-from-sim-engine
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
      if (isTaskStale(this.activeTask, this.currentTime)) {
        // Add this event to the timeline.
        this.frame.emit(Event.create(topic, event, this.activeTask));
//        if (!timeline.isTopicStale(topic, this.currentTime)) {
//          SimulationEngine.this.timeline.setTopicStale(topic, this.currentTime);
//        }
      }
      SimulationEngine.this.invalidateTopic(topic, this.currentTime);
    }

    /**
     * Return the taskId from the old simulation for the new (or old) TaskFactory.
     * @param taskFactory the TaskFactory used to create the task
     * @return the TaskId generated for the task created by taskFactory
     */
    public TaskId getOldTaskIdForDaemon(TaskFactory<?> taskFactory) {
      var taskId = oldEngine.taskIdsForFactories.get(taskFactory);
      if (taskId != null) return taskId;
      String daemonId = getMissionModel().getDaemonId(taskFactory);
      if (daemonId == null) return null;
      var oldTaskFactory = oldEngine.getMissionModel().getDaemon(daemonId);
      if (oldTaskFactory == null) return null;
      taskId = oldEngine.taskIdsForFactories.get(oldTaskFactory);
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
            // Indicate that this task is not stale by setting its stale time to Duration.MAX_VALUE.
            setTaskStale(task, Duration.MAX_VALUE);
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

  public void rescheduleTask(TaskId taskId, Duration startOffset) {
    //Look for serialized activity for task
    // If no parent is an activity, then see if it is a daemon task.
    // If it's not an activity or daemon task, report an error somehow (e.g., exception or log.error()).
    TaskId activityId = null;
    TaskId lastId = taskId;
    boolean isAct = false;
    boolean isDaemon = true;
    while (true) {
      if (oldEngine.taskInfo.isActivity(lastId)) {
        isAct = true;
        activityId = lastId;
        isDaemon = false;
        break;
      }
      var tempId = oldEngine.taskParent.get(lastId);
      if (tempId == null) {
        break;
      }
      lastId = tempId;
    }

    if (isDaemon) {
      if (!daemonTasks.contains(taskId)) {
        throw new RuntimeException("WARNING: Expected TaskId to be a daemon task: " + taskId);
      }
      TaskFactory<?> factory = oldEngine.taskFactories.get(taskId);
      if (factory != null && startOffset != null && startOffset != Duration.MAX_VALUE) {
        scheduleTask(startOffset, factory, taskId);  // TODO: Emit something like with emitAndThen() in the isAct case below?
      } else {
        throw new RuntimeException("Can't reschedule task " + taskId + " at time offset " + startOffset +
                                   (factory == null ? " because there is no TaskFactory." : "."));
      }
    } else if (isAct) {
      // Get the SerializedActivity for the taskId.
      // If an activity is found, see if it is associated with a directive and, if so, use the directive instead.
      SerializedActivity serializedActivity = this.taskInfo.input.get(activityId.id());
      var activityDirectiveId = taskInfo.taskToPlannedDirective.get(activityId.id());
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
      scheduleTask(startOffset, emitAndThen(activityDirectiveId, defaultActivityTopic, task), activityId);
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
}
