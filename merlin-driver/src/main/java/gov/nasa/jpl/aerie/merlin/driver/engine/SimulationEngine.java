package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel.SerializableTopic;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.resources.SimulationResourceManager;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.UnfinishedActivity;
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
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.types.ActivityDirectiveId;
import gov.nasa.jpl.aerie.types.SerializedActivity;
import gov.nasa.jpl.aerie.types.Timestamp;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.merlin.driver.SimulationDriver.deserializeActivity;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

/**
 * A representation of the work remaining to do during a simulation, and its accumulated results.
 */
public final class SimulationEngine implements AutoCloseable {
  private boolean closed = false;

  /** The set of all jobs waiting for time to pass. */
  private final JobSchedule<JobId, SchedulingInstant> scheduledJobs;
  /** The set of all jobs waiting on a condition. */
  private final Map<ConditionId, TaskId> waitingTasks;
  /** The set of all tasks blocked on some number of subtasks. */
  private final Map<TaskId, MutableInt> blockedTasks;
  /** The set of conditions depending on a given set of topics. */
  private final Subscriptions<Topic<?>, ConditionId> waitingConditions;
  /** The set of queries depending on a given set of topics. */
  private final Subscriptions<Topic<?>, ResourceId> waitingResources;

  /** The execution state for every in-progress task. */
  public final Map<TaskId, ExecutionState<?>> tasks;
  /** The getter for each tracked condition. */
  private final Map<ConditionId, Condition> conditions;
  /** The profiling state for each tracked resource. */
  private final Map<ResourceId, Resource<?>> resources;

  /** Keeps track of entry points for all in-progress tasks */
  public final Map<TaskId, TaskEntryPoint> entrypoints;
  public final Map<TaskId, Duration> lastStepTime;
  public final Map<TaskId, List<SerializedValue>> readLog;
  public final Map<TaskId, Integer> taskSteps;
  public final Map<TaskId, Integer> childCount;
  public final Map<TaskId, TaskId> taskParent;
  public final Map<TaskId, SpanId> taskSpan;

  /** Tasks that have been scheduled, but not started */
  private final Map<TaskId, Duration> unstartedTasks;

  /** The set of all spans of work contributed to by modeled tasks. */
  public final Map<SpanId, Span> spans;
  /** A count of the direct contributors to each span, including child spans and tasks. */
  private final Map<SpanId, MutableInt> spanContributorCount;

  /** A thread pool that modeled tasks can use to keep track of their state between steps. */
  public ExecutorService executor; // MD: not final for Matt's purposes - don't merge this way

  /* The top-level simulation timeline. */
  public final TemporalEventSource timeline;
  private final TemporalEventSource referenceTimeline;
  public final LiveCells cells;
  private Duration elapsedTime;

  public SimulationEngine(LiveCells initialCells, ExecutorService executor) {
    this(initialCells);
    this.executor = executor;
  }

  public SimulationEngine(LiveCells initialCells) {
    timeline = new TemporalEventSource();
    referenceTimeline = new TemporalEventSource();
    cells = new LiveCells(timeline, initialCells);
    elapsedTime = ZERO;

    scheduledJobs = new JobSchedule<>();
    waitingTasks = new LinkedHashMap<>();
    blockedTasks = new LinkedHashMap<>();
    waitingConditions = new Subscriptions<>();
    waitingResources = new Subscriptions<>();
    tasks = new LinkedHashMap<>();
    conditions = new LinkedHashMap<>();
    resources = new LinkedHashMap<>();
    unstartedTasks = new LinkedHashMap<>();
    spans = new LinkedHashMap<>();
    spanContributorCount = new LinkedHashMap<>();
    entrypoints = new LinkedHashMap<>();
    lastStepTime = new LinkedHashMap<>();
    readLog = new LinkedHashMap<>();
    taskSteps = new LinkedHashMap<>();
    childCount = new LinkedHashMap<>();
    taskParent = new LinkedHashMap<>();
    taskSpan = new LinkedHashMap<>();
    executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  private SimulationEngine(SimulationEngine other) {
    other.timeline.freeze();
    other.referenceTimeline.freeze();
    other.cells.freeze();

    elapsedTime = other.elapsedTime;

    timeline = new TemporalEventSource();
    cells = new LiveCells(timeline, other.cells);
    referenceTimeline = other.combineTimeline();

    // New Executor allows other SimulationEngine to be closed
    executor = Executors.newVirtualThreadPerTaskExecutor();
    scheduledJobs = other.scheduledJobs.duplicate();
    waitingTasks = new LinkedHashMap<>(other.waitingTasks);
    blockedTasks = new LinkedHashMap<>();
    for (final var entry : other.blockedTasks.entrySet()) {
      blockedTasks.put(entry.getKey(), new MutableInt(entry.getValue()));
    }
    waitingConditions = other.waitingConditions.duplicate();
    waitingResources = other.waitingResources.duplicate();
    tasks = new LinkedHashMap<>();
    for (final var entry : other.tasks.entrySet()) {
      tasks.put(entry.getKey(), entry.getValue().duplicate(executor));
    }
    conditions = new LinkedHashMap<>(other.conditions);
    resources = new LinkedHashMap<>(other.resources);
    unstartedTasks = new LinkedHashMap<>(other.unstartedTasks);
    spans = new LinkedHashMap<>(other.spans);
    spanContributorCount = new LinkedHashMap<>();
    for (final var entry : other.spanContributorCount.entrySet()) {
      spanContributorCount.put(entry.getKey(), new MutableInt(entry.getValue().getValue()));
    }
    lastStepTime = new LinkedHashMap<>(other.lastStepTime);
    readLog = new LinkedHashMap<>();
    for (final var entry : other.readLog.entrySet()) {
      readLog.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    taskSteps = new LinkedHashMap<>(other.taskSteps);
    childCount = new LinkedHashMap<>(other.childCount);
    taskParent = new LinkedHashMap<>(other.taskParent);
    taskSpan = new LinkedHashMap<>(other.taskSpan);
    entrypoints = other.entrypoints;
  }

  /** Initialize the engine by tracking resources and kicking off daemon tasks. **/
  public void init(Map<String, Resource<?>> resources, Optional<TaskFactory<Unit>> daemons) throws Throwable {
    // Begin tracking all resources.
    for (final var entry : resources.entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();

      this.trackResource(name, resource, elapsedTime);
    }

    if (daemons.isPresent()) {
      // Start daemon task(s) immediately, before anything else happens.
      this.scheduleTask(ZERO, daemons.get(), new TaskEntryPoint.Daemon(TaskEntryPoint.freshId()));
      {
        final var batch = this.extractNextJobs(Duration.MAX_VALUE);
        final var results = this.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE);
        for (final var commit : results.commits()) {
          timeline.add(commit);
        }
        if (results.error.isPresent()) {
          throw results.error.get();
        }
      }
    }
  }

  private record MyTask<T>(MutableObject<TaskFactory<T>> task, long steps, long numChildren, List<SerializedValue> readLog, Duration lastStepTime, TaskEntryPoint entrypoint, List<MyTask<?>> childrenToRestart) {
    public <F> void setTaskFactory(TaskFactory<F> taskFactory) {
      this.task.setValue((TaskFactory<T>) taskFactory);
    }
  }

  public <Model> void hydrateInitialConditions(
      final MissionModel<Model> missionModel,
      final ExecutorService executor,
      final SerializedValue incons,
      final LiveCells cells,
      final Instant simulationStartTime
  ) {
    final var tasks = new LinkedHashMap<String, MyTask<?>>();
    for (final var serializedTask : incons.asMap().get().get("tasks").asList().get()) {
      final var entrypoint = serializedTask.asMap().get().get("entrypoint").asMap().get();

      final var id = entrypoint.get("id").asString().get();

      switch (entrypoint.get("type").asString().get()) {
        case "directive" -> {
          final var directive = entrypoint.get("directive").asMap().get();
          final var startTime = Timestamp.fromString(entrypoint.get("startTime").asString().get()).toInstant();
          final var type = directive.get("type").asString().get();
          final var args = directive.get("args").asMap().get();
          final var taskFactory = deserializeActivity(missionModel, new SerializedActivity(type, args));
          final var steps = serializedTask.asMap().get().get("steps").asInt().get();
          final var numChildren = serializedTask.asMap().get().get("children").asInt().get();
          final Optional<TaskEntryPoint.ParentReference> parentReference;
          if (!entrypoint.get("parentId").isNull()) {
            parentReference = Optional.of(new TaskEntryPoint.ParentReference(entrypoint.get("parentId").asString().get(), entrypoint.get("childNumber").asInt().get()));
          } else {
            parentReference = Optional.empty();
          }
          final List<SerializedValue> readLog = serializedTask.asMap().get().get("reads").asList().get();
          final Duration lastStepTime = Duration.of(
              serializedTask.asMap().get().get("lastStepTime").asInt().get(),
              MICROSECONDS);
          tasks.put(id, new MyTask<>(new MutableObject<>(taskFactory), steps, numChildren, readLog, lastStepTime, new TaskEntryPoint.Directive(id, startTime, new SerializedActivity(type, args), parentReference), new ArrayList<>()));
        }

        case "subtask" -> {
          final var parentId = entrypoint.get("parentId").asString().get();
          final var childNumber = entrypoint.get("childNumber").asInt().get();
          final var parentReference = new TaskEntryPoint.ParentReference(parentId, childNumber);
          final var steps = serializedTask.asMap().get().get("steps").asInt().get();
          final var numChildren = serializedTask.asMap().get().get("children").asInt().get();
          final List<SerializedValue> readLog = serializedTask.asMap().get().get("reads").asList().get();
          final Duration lastStepTime = Duration.of(
              serializedTask.asMap().get().get("lastStepTime").asInt().get(),
              MICROSECONDS);
          tasks.put(id, new MyTask<>(new MutableObject<>(null), steps, numChildren, readLog, lastStepTime, new TaskEntryPoint.Subtask(id, parentReference), new ArrayList<>()));
        }

        case "system" -> {
        }

        case "daemon" -> {
          final var steps = serializedTask.asMap().get().get("steps").asInt().get();
          final var numChildren = serializedTask.asMap().get().get("children").asInt().get();
          final List<SerializedValue> readLog = serializedTask.asMap().get().get("reads").asList().get();
          final Duration lastStepTime = Duration.of(
              serializedTask.asMap().get().get("lastStepTime").asInt().get(),
              MICROSECONDS);
          tasks.put(id, new MyTask<>(new MutableObject<>(missionModel.getDaemon()), steps, numChildren, readLog, lastStepTime, new TaskEntryPoint.Daemon(id), new ArrayList<>()));
        }
      }
    }

    var rootIds = new LinkedHashSet<>(tasks.keySet());
    for (final var entry : tasks.entrySet()) {
      final var task = entry.getValue();
      if (task.entrypoint().parent().isPresent()) {
        final var parent = tasks.get(task.entrypoint().parent().get().id());
        if (parent != null) {
          parent.childrenToRestart().add(task);
          rootIds.remove(entry.getKey());
        }
        if (task.task().getValue() == null) {
          rootIds.remove(entry.getKey()); // Cannot restart this task
        }
      }
    }

    final var roots = new ArrayList<MyTask<?>>();
    for (final var taskId : rootIds) {
      roots.add(tasks.get(taskId));
    }

    final Set<Topic<?>> topics = new LinkedHashSet<>();
    for (final SerializableTopic<?> topic$ : missionModel.getTopics()) {
      if (topic$.name().startsWith("ActivityType.Input.") || topic$.name().startsWith("ActivityType.Output.")) {
        topics.add(topic$.topic());
      }
    }

    for (final var task : roots) {
      instantiateTask(Optional.empty(), Optional.empty(), task, cells, executor, topics, simulationStartTime);
    }
  }

  private <T> void instantiateTask(Optional<TaskId> caller, Optional<SpanId> parentSpan, MyTask<T> readyTask, final LiveCells cells, ExecutorService executor, Set<Topic<?>> topics, Instant simulationStartTime) {
    // Make a TaskId for this task. For convenience, let's reuse the id from the incons
    final var taskId = new TaskId(readyTask.entrypoint().id());
    final var span = SpanId.generate();
    this.spans.put(span, new Span(parentSpan, readyTask.entrypoint() instanceof TaskEntryPoint.Directive s ? Duration.of(-s.startTime().until(
        simulationStartTime, ChronoUnit.MICROS), MICROSECONDS) : ZERO, Optional.empty()));
    parentSpan.ifPresent($ -> this.spanContributorCount.get($).increment());
    this.spanContributorCount.put(span, new MutableInt(1));
    this.taskSpan.put(taskId, span); // TODO should it always be fresh? Probably not

    final var readIterator = readyTask.readLog().iterator();
    final var childCounter = new MutableLong(0);

    final var fakeScheduler = new Scheduler() {
      @Override
      public <State> State get(final CellId<State> cellId) {
        final var query = ((EngineCellId<?, State>) cellId);
        final var cell = cells.getCell(query.query());
        if (cell.isEmpty()) {
          throw new IllegalStateException();
        }
        return cells.getCell(query.query()).get().deserialize(readIterator.next()).getState();
      }

      @Override
      public <EventType> void emit(final EventType event, final Topic<EventType> topic) {
        if (topics.contains(topic)) {
          // Actually emit the event
          SimulationEngine.this.timeline.add(EventGraph.atom(Event.create(topic, event, span, taskId)));
        }
      }

      @Override
      public void spawn(final InSpan taskSpan, final TaskFactory<?> task) {
        for (final var waitingChild : readyTask.childrenToRestart()) {
          if (childCounter.getValue().equals(waitingChild.entrypoint().parent().get().childNumber())) {
            waitingChild.setTaskFactory(task);

            taskParent.put(new TaskId(waitingChild.entrypoint().id()), taskId);
            instantiateTask(caller, Optional.of(span), waitingChild, cells, executor, topics, simulationStartTime);

            final var callerBlockCount = SimulationEngine.this.blockedTasks.get(taskId);
            if (callerBlockCount != null) callerBlockCount.increment();
          }
        }
        childCounter.increment();
      }
    };
    TaskStatus<T> status = null;
    Task<T> task$ = readyTask.task().getValue().create(executor);
    for (var i = 0; i < readyTask.steps(); i++) {
      status = task$.step(fakeScheduler);
      switch (status) {
        case TaskStatus.CallingTask<T> s -> {
          for (final var waitingChild : readyTask.childrenToRestart()) {
            if (childCounter.getValue().equals(waitingChild.entrypoint().parent().get().childNumber())) {
              waitingChild.setTaskFactory(s.child());
              taskParent.put(new TaskId(waitingChild.entrypoint().id()), taskId);
              this.blockedTasks.put(taskId, new MutableInt(1)); // TODO this counter is probably wrong. Check spawn case
              instantiateTask(Optional.of(taskId), Optional.of(span), waitingChild, cells, executor, topics,
                              simulationStartTime);
            }
          }
          childCounter.increment();
        }
        case TaskStatus.AwaitingCondition<T> s -> {
          task$ = s.continuation();
        }
        case TaskStatus.Completed<T> s -> {
          var span$ = span;
          while (true) {
            if (this.spanContributorCount.get(span$).decrementAndGet() > 0) break;
            this.spanContributorCount.remove(span$);

            this.spans.compute(span$, (_id, $) -> $.close(ZERO));

            final var span$$ = this.spans.get(span$).parent;
            if (span$$.isEmpty()) break;

            span$ = span$$.get();
          }
        }
        case TaskStatus.Delayed<T> s -> {
          task$ = s.continuation();
        }
      }
    }

    this.entrypoints.put(taskId, readyTask.entrypoint());
    this.lastStepTime.put(taskId, readyTask.lastStepTime());
    this.readLog.put(taskId, new ArrayList<>(readyTask.readLog()));
    this.taskSteps.put(taskId, (int) readyTask.steps());
    this.childCount.put(taskId, (int) readyTask.numChildren());
    this.taskSpan.put(taskId, span);

    final var progress = new ExecutionState<>(span, caller, task$);

    switch (status) {
      case TaskStatus.Completed<T> s -> {
        // This task is finished; nothing to do
      }

      case TaskStatus.Delayed<T> s -> {
        if (s.delay().isNegative()) {
          throw new IllegalArgumentException("Attempted to restart a task that should have stepped in the past");
        }

        this.tasks.put(taskId, progress.continueWith(s.continuation()));
        this.scheduledJobs.schedule(JobId.forTask(taskId), SubInstant.Tasks.at(s.delay().plus(readyTask.lastStepTime())));
        this.tasks.put(taskId, progress);
      }

      case TaskStatus.CallingTask<T> s -> {
        // INVARIANT: If the above stepping code hits a CallingTask for an unfinished activity, that must be its final status
        // All the work for this case was done above.
        this.tasks.put(taskId, progress);
      }

      case TaskStatus.AwaitingCondition<T> s -> {
        final var condition = ConditionId.generate();
        this.conditions.put(condition, s.condition());
        this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(ZERO)); // TODO Can we skip triggering the condition at time 0?

        this.waitingTasks.put(condition, taskId);
        this.tasks.put(taskId, progress.continueWith(s.continuation()));
      }
    }
  }

  public sealed interface Status {
    record CompleteNoJobs() implements Status {}
    record CompleteAtDuration() implements Status{}
    record InProgress(
        Duration elapsedTime,
        Map<String, Pair<ValueSchema, RealDynamics>> realResourceUpdates,
        Map<String, Pair<ValueSchema, SerializedValue>> dynamicResourceUpdates
    ) implements Status {}
  }

  public Duration getElapsedTime() {
    return elapsedTime;
  }

  /** Step the engine forward one batch. **/
  public Status step(Duration simulationDuration) throws Throwable {
    final var nextTime = this.peekNextTime().orElse(Duration.MAX_VALUE);
    if (nextTime.longerThan(simulationDuration)) {
      elapsedTime = Duration.max(elapsedTime, simulationDuration); // avoid lowering elapsed time
      return new Status.CompleteAtDuration();
    }

    final var batch = this.extractNextJobs(simulationDuration);

    // Increment real time, if necessary.
    final var delta = batch.offsetFromStart().minus(elapsedTime);
    elapsedTime = batch.offsetFromStart();
    timeline.add(delta);

    // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
    //   even if they occur at the same real time.
    if (batch.jobs().isEmpty()) return new Status.CompleteNoJobs();

    // Run the jobs in this batch.
    final var results = this.performJobs(batch.jobs(), cells, elapsedTime, simulationDuration);
    for (final var commit : results.commits()) {
      timeline.add(commit);
    }
    if (results.error.isPresent()) {
      throw results.error.get();
    }

    // Serialize the resources updated in this batch
    final var realResourceUpdates = new HashMap<String, Pair<ValueSchema, RealDynamics>>();
    final var dynamicResourceUpdates = new HashMap<String, Pair<ValueSchema, SerializedValue>>();

    for (final var update : results.resourceUpdates.updates()) {
      final var name = update.resourceId().id();
      final var schema = update.resource().getOutputType().getSchema();

      switch (update.resource.getType()) {
        case "real" -> realResourceUpdates.put(name, Pair.of(schema, SimulationEngine.extractRealDynamics(update)));
        case "discrete" -> dynamicResourceUpdates.put(
            name,
            Pair.of(
                schema,
                SimulationEngine.extractDiscreteDynamics(update)));
      }
    }

    return new Status.InProgress(elapsedTime, realResourceUpdates, dynamicResourceUpdates);
  }

  private static <Dynamics> RealDynamics extractRealDynamics(final ResourceUpdates.ResourceUpdate<Dynamics> update) {
    final var resource = update.resource;
    final var dynamics = update.update.dynamics();

    final var serializedSegment = resource.getOutputType().serialize(dynamics).asMap().orElseThrow();
    final var initial = serializedSegment.get("initial").asReal().orElseThrow();
    final var rate = serializedSegment.get("rate").asReal().orElseThrow();

    return RealDynamics.linear(initial, rate);
  }

  private static <Dynamics> SerializedValue extractDiscreteDynamics(final ResourceUpdates.ResourceUpdate<Dynamics> update) {
    return update.resource.getOutputType().serialize(update.update.dynamics());
  }

  /** Schedule a new task to be performed at the given time. */
  public <Output> SpanId scheduleTask(final Duration startTime, final TaskFactory<Output> state, final TaskEntryPoint entrypoint) {
    if (this.closed) throw new IllegalStateException("Cannot schedule task on closed simulation engine");
    if (startTime.isNegative()) throw new IllegalArgumentException(
        "Cannot schedule a task before the start time of the simulation");

    final var span = SpanId.generate();
    this.spans.put(span, new Span(Optional.empty(), startTime, Optional.empty()));

    final var task = TaskId.generate();
    this.spanContributorCount.put(span, new MutableInt(1));
    this.tasks.put(task, new ExecutionState<>(span, Optional.empty(), state.create(this.executor)));
    this.taskSpan.put(task, span);
    this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(startTime));

    this.unstartedTasks.put(task, startTime);

    this.entrypoints.put(task, entrypoint);
    this.taskSteps.put(task, 0);
    this.childCount.put(task, 0);

    return span;
  }

  /** Register a resource whose profile should be accumulated over time. */
  public <Dynamics>
  void trackResource(final String name, final Resource<Dynamics> resource, final Duration nextQueryTime) {
    if (this.closed) throw new IllegalStateException("Cannot track resource on closed simulation engine");
    final var id = new ResourceId(name);

    this.resources.put(id, resource);
    this.scheduledJobs.schedule(JobId.forResource(id), SubInstant.Resources.at(nextQueryTime));
  }

  /** Schedules any conditions or resources dependent on the given topic to be re-checked at the given time. */
  public void invalidateTopic(final Topic<?> topic, final Duration invalidationTime) {
    if (this.closed) throw new IllegalStateException("Cannot invalidate topic on closed simulation engine");
    final var resources = this.waitingResources.invalidateTopic(topic);
    for (final var resource : resources) {
      this.scheduledJobs.schedule(JobId.forResource(resource), SubInstant.Resources.at(invalidationTime));
    }

    final var conditions = this.waitingConditions.invalidateTopic(topic);
    for (final var condition : conditions) {
      // If we were going to signal tasks on this condition, well, don't do that.
      // Schedule the condition to be rechecked ASAP.
      this.scheduledJobs.unschedule(JobId.forSignal(condition));
      this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(invalidationTime));
    }
  }

  /** Removes and returns the next set of jobs to be performed concurrently. */
  public JobSchedule.Batch<JobId> extractNextJobs(final Duration maximumTime) {
    if (this.closed) throw new IllegalStateException("Cannot extract next jobs on closed simulation engine");
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

  public record ResourceUpdates(List<ResourceUpdate<?>> updates) {
    public boolean isEmpty() {
      return updates.isEmpty();
    }

    public int size() {
      return updates.size();
    }

    ResourceUpdates() {
      this(new ArrayList<>());
    }

    public <Dynamics> void add(ResourceUpdate<Dynamics> update) {
      this.updates.add(update);
    }

    public record ResourceUpdate<Dynamics>(
        ResourceId resourceId,
        Resource<Dynamics> resource,
        Update<Dynamics> update
    ) {
      public record Update<Dynamics>(Duration startOffset, Dynamics dynamics) {}

      public ResourceUpdate(
          final Querier querier,
          final Duration currentTime,
          final ResourceId resourceId,
          final Resource<Dynamics> resource
      ) {
        this(resourceId, resource, new Update<>(currentTime, resource.getDynamics(querier)));
      }
    }
  }

  public record StepResult(
      List<EventGraph<Event>> commits,
      ResourceUpdates resourceUpdates,
      Optional<Throwable> error
  ) {}

  /** Performs a collection of tasks concurrently, extending the given timeline by their stateful effects. */
  public StepResult performJobs(
      final Collection<JobId> jobs,
      final LiveCells context,
      final Duration currentTime,
      final Duration maximumTime
  ) throws SpanException {
    if (this.closed) throw new IllegalStateException("Cannot perform jobs on closed simulation engine");
    var tip = EventGraph.<Event>empty();
    Mutable<Optional<Throwable>> exception = new MutableObject<>(Optional.empty());
    final var resourceUpdates = new ResourceUpdates();
    for (final var job$ : jobs) {
      tip = EventGraph.concurrently(tip, TaskFrame.run(job$, context, (job, frame) -> {
        try {
          this.performJob(job, frame, currentTime, maximumTime, resourceUpdates);
        } catch (Throwable ex) {
          exception.setValue(Optional.of(ex));
        }
      }));

      if (exception.getValue().isPresent()) {
        return new StepResult(List.of(tip), resourceUpdates, exception.getValue());
      }
    }
    return new StepResult(List.of(tip), resourceUpdates, Optional.empty());
  }

  /** Performs a single job. */
  public void performJob(
      final JobId job,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final Duration maximumTime,
      final ResourceUpdates resourceUpdates
  ) throws SpanException {
    switch (job) {
      case JobId.TaskJobId j -> this.stepTask(j.id(), frame, currentTime);
      case JobId.SignalJobId j -> this.stepTask(this.waitingTasks.remove(j.id()), frame, currentTime);
      case JobId.ConditionJobId j -> this.updateCondition(j.id(), frame, currentTime, maximumTime);
      case JobId.ResourceJobId j -> this.updateResource(j.id(), frame, currentTime, resourceUpdates);
      case null -> throw new IllegalArgumentException("Unexpected null value for JobId");
      default -> throw new IllegalArgumentException("Unexpected subtype of %s: %s".formatted(
          JobId.class,
          job.getClass()));
    }
  }

  /** Perform the next step of a modeled task. */
  public void stepTask(final TaskId task, final TaskFrame<JobId> frame, final Duration currentTime)
  throws SpanException {
    if (this.closed) throw new IllegalStateException("Cannot step task on closed simulation engine");
    this.unstartedTasks.remove(task);
    this.lastStepTime.put(task, currentTime);
    this.taskSteps.putIfAbsent(task, 0);
    this.taskSteps.put(task, this.taskSteps.get(task) + 1);
    // The handler for the next status of the task is responsible
    //   for putting an updated state back into the task set.
    var state = this.tasks.remove(task);

    stepEffectModel(task, state, frame, currentTime);
  }

  /** Make progress in a task by stepping its associated effect model forward. */
  private <Output> void stepEffectModel(
      final TaskId task,
      final ExecutionState<Output> progress,
      final TaskFrame<JobId> frame,
      final Duration currentTime
  ) throws SpanException {
    // Step the modeling state forward.
    final var scheduler = new EngineScheduler(currentTime, progress.span(), task, progress.caller(), frame);
    final TaskStatus<Output> status;
    try {
      status = progress.state().step(scheduler);
    } catch (Throwable ex) {
      throw new SpanException(scheduler.span, ex);
    }
    // TODO: Report which topics this activity wrote to at this point in time. This is useful insight for any user.
    // TODO: Report which cells this activity read from at this point in time. This is useful insight for any user.

    // Based on the task's return status, update its execution state and schedule its resumption.
    switch (status) {
      case TaskStatus.Completed<Output> s -> {
        // Propagate completion up the span hierarchy.
        // TERMINATION: The span hierarchy is a finite tree, so eventually we find a parentless span.
        var span = scheduler.span;
        while (true) {
          if (this.spanContributorCount.get(span) == null) {
            throw new IllegalStateException();
          }
          if (this.spanContributorCount.get(span).decrementAndGet() > 0) break;
          this.spanContributorCount.remove(span);

          this.spans.compute(span, (_id, $) -> $.close(currentTime));

          final var span$ = this.spans.get(span).parent;
          if (span$.isEmpty()) break;

          span = span$.get();
        }

        // Notify any blocked caller of our completion.
        progress.caller().ifPresent($ -> {
          if (this.blockedTasks.get($).decrementAndGet() == 0) {
            this.blockedTasks.remove($);
            this.scheduledJobs.schedule(JobId.forTask($), SubInstant.Tasks.at(currentTime));
          }
        });
      }

      case TaskStatus.Delayed<Output> s -> {
        if (s.delay().isNegative()) {
          throw new IllegalArgumentException("Cannot schedule a task in the past");
        }

        this.tasks.put(task, progress.continueWith(s.continuation()));
        this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime.plus(s.delay())));
      }

      case TaskStatus.CallingTask<Output> s -> {
        // Prepare a span for the child task.
        final var childSpan = switch (s.childSpan()) {
          case Parent -> scheduler.span;

          case Fresh -> {
            final var freshSpan = SpanId.generate();
            SimulationEngine.this.spans.put(
                freshSpan,
                new Span(Optional.of(scheduler.span), currentTime, Optional.empty()));
            SimulationEngine.this.spanContributorCount.put(freshSpan, new MutableInt(1));
            yield freshSpan;
          }
        };

        // Spawn the child task.
        final var childTask = TaskId.generate();
        entrypoints.put(childTask, new TaskEntryPoint.Subtask(
            TaskEntryPoint.freshId(),
            new TaskEntryPoint.ParentReference(entrypoints.get(task).id(), childCount.get(task))));

        childCount.put(childTask, 0);
        childCount.computeIfPresent(task, (taskId, oldCount) -> oldCount + 1);
        taskParent.put(childTask, task);

        SimulationEngine.this.spanContributorCount.get(scheduler.span).increment();
        SimulationEngine.this.tasks.put(
            childTask,
            new ExecutionState<>(
                childSpan,
                Optional.of(task),
                s.child().create(this.executor)));
        taskSpan.put(childTask, childSpan);
        frame.signal(JobId.forTask(childTask));

        // Arrange for the parent task to resume.... later.
        SimulationEngine.this.blockedTasks.put(task, new MutableInt(1));
        this.tasks.put(task, progress.continueWith(s.continuation()));
      }

      case TaskStatus.AwaitingCondition<Output> s -> {
        final var condition = ConditionId.generate();
        this.conditions.put(condition, s.condition());
        this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(currentTime));

        this.tasks.put(task, progress.continueWith(s.continuation()));
        this.waitingTasks.put(condition, task);
      }
    }
  }

  /** Determine when a condition is next true, and schedule a signal to be raised at that time. */
  public void updateCondition(
      final ConditionId condition,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final Duration horizonTime
  ) {
    if (this.closed) throw new IllegalStateException("Cannot update condition on closed simulation engine");
    final var querier = new EngineQuerier(frame);
    final var prediction = this.conditions
        .get(condition)
        .nextSatisfied(querier, horizonTime.minus(currentTime))
        .map(currentTime::plus);

    this.waitingConditions.subscribeQuery(condition, querier.referencedTopics);

    final var expiry = querier.expiry.map(currentTime::plus);
    if (prediction.isPresent() && (expiry.isEmpty() || prediction.get().shorterThan(expiry.get()))) {
      this.scheduledJobs.schedule(JobId.forSignal(condition), SubInstant.Tasks.at(prediction.get()));
    } else {
      // Try checking again later -- where "later" is in some non-zero amount of time!
      final var nextCheckTime = Duration.max(expiry.orElse(horizonTime), currentTime.plus(Duration.EPSILON));
      this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(nextCheckTime));
    }
  }

  /** Get the current behavior of a given resource and accumulate it into the resource's profile. */
  public void updateResource(
      final ResourceId resourceId,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final ResourceUpdates resourceUpdates) {
    if (this.closed) throw new IllegalStateException("Cannot update resource on closed simulation engine");
    final var querier = new EngineQuerier(frame);
    resourceUpdates.add(new ResourceUpdates.ResourceUpdate<>(
        querier,
        currentTime,
        resourceId,
        this.resources.get(resourceId)));

    this.waitingResources.subscribeQuery(resourceId, querier.referencedTopics);

    final var expiry = querier.expiry.map(currentTime::plus);
    if (expiry.isPresent()) {
      this.scheduledJobs.schedule(JobId.forResource(resourceId), SubInstant.Resources.at(expiry.get()));
    }
  }

  /** Resets all tasks (freeing any held resources). The engine should not be used after being closed. */
  @Override
  public void close() {
    cells.freeze();
    timeline.freeze();

    for (final var task : this.tasks.values()) {
      task.state().release();
    }

    this.executor.shutdownNow();
    this.closed = true;
  }

  public void unscheduleAfter(final Duration duration) {
    if (this.closed) throw new IllegalStateException("Cannot unschedule jobs on closed simulation engine");
    for (final var taskId : new ArrayList<>(this.tasks.keySet())) {
      if (this.unstartedTasks.containsKey(taskId) && this.unstartedTasks.get(taskId).longerThan(duration)) {
        this.tasks.remove(taskId);
        this.scheduledJobs.unschedule(JobId.forTask(taskId));
      }
    }
  }

  public record SpanInfo(
      Map<SpanId, ActivityDirectiveId> spanToPlannedDirective,
      Map<SpanId, SerializedActivity> input,
      Map<SpanId, SerializedValue> output,
      Set<TaskId> activityTasks
      ) {
    public SpanInfo() {
      this(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashSet<>());
    }

    public boolean isActivity(final SpanId id) {
      return this.input.containsKey(id);// || this.output.containsKey(id);
    }

    public boolean isActivity(final TaskId id) {
      return this.activityTasks.contains(id);
    }

    public boolean isDirective(SpanId id) {
      return this.spanToPlannedDirective.containsKey(id);
    }

    public ActivityDirectiveId getDirective(SpanId id) {
      return this.spanToPlannedDirective.get(id);
    }

    public record Trait(Iterable<SerializableTopic<?>> topics, Topic<ActivityDirectiveId> activityTopic)
        implements EffectTrait<Consumer<SpanInfo>>
    {
      @Override
      public Consumer<SpanInfo> empty() {
        return spanInfo -> {};
      }

      @Override
      public Consumer<SpanInfo> sequentially(final Consumer<SpanInfo> prefix, final Consumer<SpanInfo> suffix) {
        return spanInfo -> {
          prefix.accept(spanInfo);
          suffix.accept(spanInfo);
        };
      }

      @Override
      public Consumer<SpanInfo> concurrently(final Consumer<SpanInfo> left, final Consumer<SpanInfo> right) {
        // SAFETY: `left` and `right` should commute. HOWEVER, if a span happens to directly contain two activities
        //   -- that is, two activities both contribute events under the same span's provenance -- then this
        //   does not actually commute.
        //   Arguably, this is a model-specific analysis anyway, since we're looking for specific events
        //   and inferring model structure from them, and at this time we're only working with models
        //   for which every activity has a span to itself.
        return spanInfo -> {
          left.accept(spanInfo);
          right.accept(spanInfo);
        };
      }

      public Consumer<SpanInfo> atom(final Event ev) {
        return spanInfo -> {
          // Identify activities.
          ev.extract(this.activityTopic)
            .ifPresent(directiveId -> spanInfo.spanToPlannedDirective.put(ev.provenance(), directiveId));

          for (final var topic : this.topics) {
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
              ev.provenance(),
              new SerializedActivity(activityType, topic.outputType().serialize(input).asMap().orElseThrow()));

          spanInfo.activityTasks.add(ev.emitter());
        });
      }

      private static <T>
      void extractOutput(final SerializableTopic<T> topic, final Event ev, final SpanInfo spanInfo) {
        if (!topic.name().startsWith("ActivityType.Output.")) return;

        ev.extract(topic.topic()).ifPresent(output -> {
          spanInfo.output.put(
              ev.provenance(),
              topic.outputType().serialize(output));
          spanInfo.activityTasks.add(ev.emitter());
        });
      }
    }
  }


  /**
   * Get an Activity Directive Id from a SpanId, if the span is a descendent of a directive.
   */
  public DirectiveDetail getDirectiveDetailsFromSpan(
      final Topic<ActivityDirectiveId> activityTopic,
      final Iterable<SerializableTopic<?>> serializableTopics,
      final SpanId spanId
  ) {
    // Collect per-span information from the event graph.
    final var spanInfo = computeSpanInfo(activityTopic, serializableTopics, this.timeline);

    // Identify the nearest ancestor directive by walking up the parent
    // span tree. Save the activity trace along the way
    Optional<SpanId> directiveSpanId = Optional.of(spanId);
    final var activityStackTrace = new LinkedList<SerializedActivity>();
    while (directiveSpanId.isPresent() && !spanInfo.isDirective(directiveSpanId.get())) {
      activityStackTrace.add(spanInfo.input().get(directiveSpanId.get()));
      directiveSpanId = this.getSpan(directiveSpanId.get()).parent();
    }

    // Add final top level parent activity to the stack trace if present
    if (directiveSpanId.isPresent()) {
      activityStackTrace.add(spanInfo.input().get(directiveSpanId.get()));
    }

    return new DirectiveDetail(
        directiveSpanId.map(spanInfo::getDirective),
        // remove null activities from the stack trace and reverse order
        activityStackTrace.stream().filter(Objects::nonNull).collect(Collectors.toList()).reversed());
  }

  public record SimulationActivityExtract(
      Instant startTime,
      Duration duration,
      Map<ActivityInstanceId, ActivityInstance> simulatedActivities,
      Map<ActivityInstanceId, UnfinishedActivity> unfinishedActivities
  ) {}

  public SpanInfo computeSpanInfo(
      final Topic<ActivityDirectiveId> activityTopic,
      final Iterable<SerializableTopic<?>> serializableTopics,
      final TemporalEventSource timeline
  ) {
    // Collect per-span information from the event graph.
    final var spanInfo = new SpanInfo();

    for (final var point : timeline) {
      if (!(point instanceof TemporalEventSource.TimePoint.Commit p)) continue;

      final var trait = new SpanInfo.Trait(serializableTopics, activityTopic);
      p.events().evaluate(trait, trait::atom).accept(spanInfo);
    }
    return spanInfo;
  }

  public SimulationActivityExtract computeActivitySimulationResults(
      final Instant startTime,
      final Topic<ActivityDirectiveId> activityTopic,
      final Iterable<SerializableTopic<?>> serializableTopics
  ) {
    return computeActivitySimulationResults(
        startTime,
        computeSpanInfo(activityTopic, serializableTopics, combineTimeline())
    );
  }

  private HashMap<SpanId, ActivityDirectiveId> spanToActivityDirectiveId(
      final SpanInfo spanInfo
  )
  {
    final var activityDirectiveIds = new HashMap<SpanId, ActivityDirectiveId>();
    this.spans.forEach((span, state) -> {
      if (!spanInfo.isActivity(span)) return;
      if (spanInfo.isDirective(span)) activityDirectiveIds.put(span, spanInfo.getDirective(span));
    });
    return activityDirectiveIds;
  }

  private HashMap<SpanId, ActivityInstanceId> spanToSimulatedActivities(
      final SpanInfo spanInfo
  ) {
    final var activityDirectiveIds = spanToActivityDirectiveId(spanInfo);
    final var spanToActivityInstanceId = new HashMap<SpanId, ActivityInstanceId>(activityDirectiveIds.size());
    final var usedActivityInstanceIds = new HashSet<>();
    for (final var entry : activityDirectiveIds.entrySet()) {
      spanToActivityInstanceId.put(entry.getKey(), new ActivityInstanceId(entry.getValue().id()));
      usedActivityInstanceIds.add(entry.getValue().id());
    }
    long counter = 1L;
    for (final var span : this.spans.keySet()) {
      if (!spanInfo.isActivity(span)) continue;
      if (spanToActivityInstanceId.containsKey(span)) continue;

      while (usedActivityInstanceIds.contains(counter)) counter++;
      spanToActivityInstanceId.put(span, new ActivityInstanceId(counter++));
    }
    return spanToActivityInstanceId;
  }

  /**
   * Computes only activity-related results when resources are not needed
   */
  public SimulationActivityExtract computeActivitySimulationResults(
      final Instant startTime,
      final SpanInfo spanInfo
  ) {
    // Identify the nearest ancestor *activity* (excluding intermediate anonymous tasks).
    final var activityParents = new HashMap<SpanId, SpanId>();
    final var activityDirectiveIds = spanToActivityDirectiveId(spanInfo);
    this.spans.forEach((span, state) -> {
      if (!spanInfo.isActivity(span)) return;

      var parent = state.parent();
      while (parent.isPresent() && !spanInfo.isActivity(parent.get())) {
        parent = this.spans.get(parent.get()).parent();
      }
      parent.ifPresent(spanId -> activityParents.put(span, spanId));
    });

    final var activityChildren = new HashMap<SpanId, List<SpanId>>();
    activityParents.forEach((activity, parent) -> {
      activityChildren.computeIfAbsent(parent, $ -> new LinkedList<>()).add(activity);
    });

    // Give every task corresponding to a child activity an ID that doesn't conflict with any root activity.
    final var spanToActivityInstanceId = spanToSimulatedActivities(spanInfo);

    final var simulatedActivities = new HashMap<ActivityInstanceId, ActivityInstance>();
    final var unfinishedActivities = new HashMap<ActivityInstanceId, UnfinishedActivity>();
    this.spans.forEach((span, state) -> {
      if (!spanInfo.isActivity(span)) return;

      final var activityId = spanToActivityInstanceId.get(span);
      final var directiveId = activityDirectiveIds.get(span);

      if (state.endOffset().isPresent()) {
        final var inputAttributes = spanInfo.input().get(span);
        final var outputAttributes = spanInfo.output().get(span);

        simulatedActivities.put(activityId, new ActivityInstance(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(state.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            state.endOffset().get().minus(state.startOffset()),
            spanToActivityInstanceId.get(activityParents.get(span)),
            activityChildren
                .getOrDefault(span, Collections.emptyList())
                .stream()
                .map(spanToActivityInstanceId::get)
                .toList(),
            (activityParents.containsKey(span)) ? Optional.empty() : Optional.ofNullable(directiveId),
            outputAttributes
        ));
      } else {
        final var inputAttributes = spanInfo.input().get(span);
        unfinishedActivities.put(activityId, new UnfinishedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(state.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            spanToActivityInstanceId.get(activityParents.get(span)),
            activityChildren
                .getOrDefault(span, Collections.emptyList())
                .stream()
                .map(spanToActivityInstanceId::get)
                .toList(),
            (activityParents.containsKey(span)) ? Optional.empty() : Optional.ofNullable(directiveId)
        ));
      }
    });
    return new SimulationActivityExtract(startTime, elapsedTime, simulatedActivities, unfinishedActivities);
  }

  private TreeMap<Duration, List<EventGraph<EventRecord>>> createSerializedTimeline(
      final TemporalEventSource combinedTimeline,
      final Iterable<SerializableTopic<?>> serializableTopics,
      final HashMap<SpanId, ActivityInstanceId> spanToActivities,
      final HashMap<SerializableTopic<?>, Integer> serializableTopicToId) {
    final var serializedTimeline = new TreeMap<Duration, List<EventGraph<EventRecord>>>();
    var time = ZERO;
    for (var point : combinedTimeline.points()) {
      if (point instanceof TemporalEventSource.TimePoint.Delta delta) {
        time = time.plus(delta.delta());
      } else if (point instanceof TemporalEventSource.TimePoint.Commit commit) {
        final var serializedEventGraph = commit.events().substitute(
            event -> {
              // TODO can we do this more efficiently?
              EventGraph<EventRecord> output = EventGraph.empty();
              for (final var serializableTopic : serializableTopics) {
                Optional<SerializedValue> serializedEvent = trySerializeEvent(event, serializableTopic);
                if (serializedEvent.isPresent()) {
                  // If the event's `provenance` has no simulated activity id, search its ancestors to find the nearest
                  // simulated activity id, if one exists
                  if (!spanToActivities.containsKey(event.provenance())) {
                    var spanId = Optional.of(event.provenance());

                    while (true) {
                      if (spanToActivities.containsKey(spanId.get())) {
                        spanToActivities.put(event.provenance(), spanToActivities.get(spanId.get()));
                        break;
                      }
                      spanId = this.getSpan(spanId.get()).parent();
                      if (spanId.isEmpty()) {
                        break;
                      }
                    }
                  }
                  var activitySpanID = Optional.ofNullable(spanToActivities.get(event.provenance())).map(ActivityInstanceId::id);
                  output = EventGraph.concurrently(
                      output,
                      EventGraph.atom(
                          new EventRecord(serializableTopicToId.get(serializableTopic),
                                          activitySpanID,
                                          serializedEvent.get())));
                }
              }
              return output;
            }
        ).evaluate(new EventGraph.IdentityTrait<>(), EventGraph::atom);
        if (!(serializedEventGraph instanceof EventGraph.Empty)) {
          serializedTimeline
              .computeIfAbsent(time, x -> new ArrayList<>())
              .add(serializedEventGraph);
        }
      }
    }
    return serializedTimeline;
  }


  /** Compute a set of results from the current state of simulation. */
  // TODO: Move result extraction out of the SimulationEngine.
  //   The Engine should only need to stream events of interest to a downstream consumer.
  //   The Engine cannot be cognizant of all downstream needs.
  // TODO: Whatever mechanism replaces `computeResults` also ought to replace `isTaskComplete`.
  // TODO: Produce results for all tasks, not just those that have completed.
  //   Planners need to be aware of failed or unfinished tasks.
  public SimulationResults computeResults (
      final Instant startTime,
      final Topic<ActivityDirectiveId> activityTopic,
      final Iterable<SerializableTopic<?>> serializableTopics,
      final SimulationResourceManager resourceManager
  ) {
    final var combinedTimeline = this.combineTimeline();
    // Collect per-task information from the event graph.
    final var spanInfo = computeSpanInfo(activityTopic, serializableTopics, combinedTimeline);

    // Extract profiles for every resource.
    final var resourceProfiles = resourceManager.computeProfiles(elapsedTime);
    final var realProfiles = resourceProfiles.realProfiles();
    final var discreteProfiles = resourceProfiles.discreteProfiles();

    final var activityResults = computeActivitySimulationResults(startTime, spanInfo);

    final List<Triple<Integer, String, ValueSchema>> topics = new ArrayList<>();
    final var serializableTopicToId = new HashMap<SerializableTopic<?>, Integer>();
    for (final var serializableTopic : serializableTopics) {
      serializableTopicToId.put(serializableTopic, topics.size());
      topics.add(Triple.of(topics.size(), serializableTopic.name(), serializableTopic.outputType().getSchema()));
    }

    final var serializedTimeline = createSerializedTimeline(
        combinedTimeline,
        serializableTopics,
        spanToSimulatedActivities(spanInfo),
        serializableTopicToId
    );

    return new SimulationResults(
        realProfiles,
        discreteProfiles,
        activityResults.simulatedActivities,
        activityResults.unfinishedActivities,
        startTime,
        elapsedTime,
        topics,
        serializedTimeline);
  }

  public SimulationResults computeResults(
      final Instant startTime,
      final Topic<ActivityDirectiveId> activityTopic,
      final Iterable<SerializableTopic<?>> serializableTopics,
      final SimulationResourceManager resourceManager,
      final Set<String> resourceNames
  ) {
    final var combinedTimeline = this.combineTimeline();
    // Collect per-task information from the event graph.
    final var spanInfo = computeSpanInfo(activityTopic, serializableTopics, combinedTimeline);

    // Extract profiles for every resource.
    final var resourceProfiles = resourceManager.computeProfiles(elapsedTime, resourceNames);
    final var realProfiles = resourceProfiles.realProfiles();
    final var discreteProfiles = resourceProfiles.discreteProfiles();

    final var activityResults = computeActivitySimulationResults(startTime, spanInfo);

    final List<Triple<Integer, String, ValueSchema>> topics = new ArrayList<>();
    final var serializableTopicToId = new HashMap<SerializableTopic<?>, Integer>();
    for (final var serializableTopic : serializableTopics) {
      serializableTopicToId.put(serializableTopic, topics.size());
      topics.add(Triple.of(topics.size(), serializableTopic.name(), serializableTopic.outputType().getSchema()));
    }

    final var serializedTimeline = createSerializedTimeline(
        combinedTimeline,
        serializableTopics,
        spanToSimulatedActivities(spanInfo),
        serializableTopicToId
    );

    return new SimulationResults(
        realProfiles,
        discreteProfiles,
        activityResults.simulatedActivities,
        activityResults.unfinishedActivities,
        startTime,
        elapsedTime,
        topics,
        serializedTimeline);
  }

  public Span getSpan(SpanId spanId) {
    return this.spans.get(spanId);
  }


  private static <EventType> Optional<SerializedValue> trySerializeEvent(
      Event event,
      SerializableTopic<EventType> serializableTopic
  ) {
    return event.extract(serializableTopic.topic(), serializableTopic.outputType()::serialize);
  }

  /** A handle for processing requests from a modeled resource or condition. */
  private static final class EngineQuerier implements Querier {
    private final TaskFrame<JobId> frame;
    private final Set<Topic<?>> referencedTopics = new HashSet<>();
    private Optional<Duration> expiry = Optional.empty();

    public EngineQuerier(final TaskFrame<JobId> frame) {
      this.frame = Objects.requireNonNull(frame);
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

      return state$.orElseThrow(IllegalArgumentException::new);
    }

    private static Optional<Duration> min(final Optional<Duration> a, final Optional<Duration> b) {
      if (a.isEmpty()) return b;
      if (b.isEmpty()) return a;
      return Optional.of(Duration.min(a.get(), b.get()));
    }
  }

  /** A handle for processing requests and effects from a modeled task. */
  public final class EngineScheduler implements Scheduler {
    private final Duration currentTime;
    private final SpanId span;
    private final TaskId taskId;
    private final Optional<TaskId> caller;
    private final TaskFrame<JobId> frame;

    public EngineScheduler(
        final Duration currentTime,
        final SpanId span,
        final TaskId taskId,
        final Optional<TaskId> caller,
        final TaskFrame<JobId> frame)
    {
      this.currentTime = Objects.requireNonNull(currentTime);
      this.span = Objects.requireNonNull(span);
      this.caller = Objects.requireNonNull(caller);
      this.frame = Objects.requireNonNull(frame);
      this.taskId = taskId;
    }

    @Override
    public <State> State get(final CellId<State> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via MissionModelBuilder).
      @SuppressWarnings("unchecked")
      final var query = ((EngineCellId<?, State>) token);

      // TODO: Cache the return value (until the next emit or until the task yields) to avoid unnecessary copies
      //  if the same state is requested multiple times in a row.
      final var state$ = this.frame.getState(query.query());

      readLog.computeIfAbsent(taskId, $ -> new ArrayList<>()).add(this.frame.serialize(query.query()));

      return state$.orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public <EventType> void emit(final EventType event, final Topic<EventType> topic) {
      // Append this event to the timeline.
      this.frame.emit(Event.create(topic, event, this.span, this.taskId));

      SimulationEngine.this.invalidateTopic(topic, this.currentTime);
    }

    @Override
    public void spawn(final InSpan inSpan, final TaskFactory<?> state) {
      spawn(inSpan, state, new TaskEntryPoint.Subtask(TaskEntryPoint.freshId(), new TaskEntryPoint.ParentReference(entrypoints.get(taskId).id(), childCount.get(taskId))));
    }

    public void spawn(final InSpan inSpan, final TaskFactory<?> state, final TaskEntryPoint entrypoint) {
      // Prepare a span for the child task
      final var childSpan = switch (inSpan) {
        case Parent -> this.span;

        case Fresh -> {
          final var freshSpan = SpanId.generate();
          SimulationEngine.this.spans.put(freshSpan, new Span(Optional.of(this.span), currentTime, Optional.empty()));
          SimulationEngine.this.spanContributorCount.put(freshSpan, new MutableInt(1));
          yield freshSpan;
        }
      };

      final var childTask = TaskId.generate();

      entrypoints.put(childTask, entrypoint);
      childCount.put(childTask, 0);
      childCount.computeIfPresent(taskId, (taskId, oldCount) -> oldCount + 1);
      taskParent.put(childTask, taskId);

      SimulationEngine.this.spanContributorCount.get(this.span).increment();
      SimulationEngine.this.tasks.put(
          childTask,
          new ExecutionState<>(
              childSpan,
              this.caller,
              state.create(SimulationEngine.this.executor)));
      taskSpan.put(childTask, childSpan);
      this.frame.signal(JobId.forTask(childTask));

      this.caller.ifPresent($ -> SimulationEngine.this.blockedTasks.get($).increment());
    }

    public SimulationEngine engine() {
      return SimulationEngine.this;
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
  public record ExecutionState<Output>(SpanId span, Optional<TaskId> caller, Task<Output> state) {
    public ExecutionState<Output> continueWith(final Task<Output> newState) {
      return new ExecutionState<>(this.span, this.caller, newState);
    }

    public ExecutionState<Output> duplicate(Executor executor) {
      return new ExecutionState<>(span, caller, state.duplicate(executor));
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

  public boolean spanIsComplete(SpanId spanId) {
    return this.spans.get(spanId).isComplete();
  }

  public SimulationEngine duplicate() {
    return new SimulationEngine(this);
  }

  public Optional<Duration> peekNextTime() {
    return this.scheduledJobs.peekNextTime();
  }

  /**
   * Create a timeline that in the output of the engine's reference timeline combined with its expanded timeline.
   */
  public TemporalEventSource combineTimeline() {
    final TemporalEventSource combinedTimeline = new TemporalEventSource();
    for (final var timePoint : referenceTimeline.points()) {
      if (timePoint instanceof TemporalEventSource.TimePoint.Delta t) {
        combinedTimeline.add(t.delta());
      } else if (timePoint instanceof TemporalEventSource.TimePoint.Commit t) {
        combinedTimeline.add(t.events());
      }
    }

    for (final var timePoint : timeline) {
      if (timePoint instanceof TemporalEventSource.TimePoint.Delta t) {
        combinedTimeline.add(t.delta());
      } else if (timePoint instanceof TemporalEventSource.TimePoint.Commit t) {
        combinedTimeline.add(t.events());
      }
    }
    return combinedTimeline;
  }
}
