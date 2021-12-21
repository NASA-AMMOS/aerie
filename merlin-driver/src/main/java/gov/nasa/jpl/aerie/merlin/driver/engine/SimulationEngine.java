package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Event;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskReturnValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * A representation of the work remaining to do during a simulation, and its accumulated results.
 */
public final class SimulationEngine implements AutoCloseable {
  /** The set of all jobs waiting for time to pass. */
  private final JobSchedule<JobId, SchedulingInstant> scheduledJobs = new JobSchedule<>();
  /** The set of all jobs waiting on a given signal. */
  private final Subscriptions<SignalId, TaskId> waitingTasks = new Subscriptions<>();
  /** The set of conditions depending on a given set of topics. */
  private final Subscriptions<Topic<?>, ConditionId> waitingConditions = new Subscriptions<>();
  /** The set of queries depending on a given set of topics. */
  private final Subscriptions<Topic<?>, ResourceId> waitingResources = new Subscriptions<>();

  /** The execution state for every task. */
  private final Map<TaskId, ExecutionState> tasks = new HashMap<>();
  /** The getter for each tracked condition. */
  private final Map<ConditionId, Condition> conditions = new HashMap<>();
  /** The profiling state for each tracked resource. */
  private final Map<ResourceId, ProfilingState<?>> resources = new HashMap<>();

  /** The task that spawned a given task (if any)). */
  private final Map<TaskId, TaskId> taskParent = new HashMap<>();
  /** The set of children for each task (if any). */
  @DerivedFrom("taskParent")
  private final Map<TaskId, Set<TaskId>> taskChildren = new HashMap<>();
  /** The instantiated input provided to the task. Missing entries indicate tasks without input. */
  private final Map<TaskId, Directive<?, ?>> taskDirective = new HashMap<>();

  /**
   * Return values from tasks.
   */
  private final Map<TaskId, TaskReturnValue> taskReturns = new HashMap<>();

  /** Construct a task defined by the behavior of a model given a type and arguments. */
  public <Model>
  TaskId initiateTaskFromInput(final MissionModel<Model> model, final SerializedActivity input) {
    final var task = TaskId.generate();

    final Directive<Model, ?> directive;
    try {
      directive = model.instantiateDirective(input);
    } catch (final TaskSpecType.UnconstructableTaskSpecException ex) {
      // TODO: Provide more information about the failure.
      this.tasks.put(task, new ExecutionState.IllegalSource());

      return task;
    }

    this.tasks.put(task, new ExecutionState.NotStarted(() -> directive.createTask(model.getModel())));
    this.taskDirective.put(task, directive);

    return task;
  }

  /** Define a task given a factory method from which that task can be constructed. */
  public TaskId initiateTaskFromSource(final TaskSource source) {
    final var task = TaskId.generate();
    this.tasks.put(task, new ExecutionState.NotStarted(source));
    return task;
  }

  /** Define a task given a black-box task state. */
  public TaskId initiateTask(final Duration startTime, final Task state) {
    final var task = TaskId.generate();
    this.tasks.put(task, new ExecutionState.InProgress(startTime, state));
    return task;
  }

  /** Register a resource whose profile should be accumulated over time. */
  public <Dynamics>
  void trackResource(final String name, final Resource<Dynamics> resource, final Duration nextQueryTime) {
    final var id = new ResourceId(name);

    this.resources.put(id, ProfilingState.create(resource));
    this.scheduledJobs.schedule(JobId.forResource(id), SubInstant.Resources.at(nextQueryTime));
  }

  /** Schedule a task to be performed at the given time. Overrides any existing scheduling for the given task. */
  public void scheduleTask(final TaskId task, final Duration scheduleTime) {
    this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(scheduleTime));
  }

  /** Schedules any conditions or resources dependent on the given topic to be re-checked at the given time. */
  public void invalidateTopic(final Topic<?> topic, final Duration invalidationTime) {
    final var resources = this.waitingResources.invalidateTopic(topic);
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

  /** Removes and returns the next set of jobs to be performed concurrently. */
  public JobSchedule.Batch<JobId> extractNextJobs(final Duration maximumTime) {
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

    return batch;
  }

  /** Performs a collection of tasks concurrently, extending the given timeline by their stateful effects. */
  public EventGraph<Event> performJobs(
      final Collection<JobId> jobs,
      final LiveCells context,
      final Duration currentTime,
      final Duration maximumTime,
      final MissionModel<?> model
  ) {
    var tip = EventGraph.<Event>empty();
    for (final var job$ : jobs) {
      tip = EventGraph.concurrently(tip, TaskFrame.run(job$, context, (job, frame) -> {
        this.performJob(job, frame, currentTime, maximumTime, model);
      }));
    }

    return tip;
  }

  /** Performs a single job. */
  public void performJob(
      final JobId job,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final Duration maximumTime,
      final MissionModel<?> model
  ) {
    if (job instanceof JobId.TaskJobId j) {
      this.stepTask(j.id(), frame, currentTime, model);
    } else if (job instanceof JobId.SignalJobId j) {
      this.stepSignalledTasks(j.id(), frame);
    } else if (job instanceof JobId.ConditionJobId j) {
      this.updateCondition(j.id(), frame, currentTime, maximumTime);
    } else if (job instanceof JobId.ResourceJobId j) {
      this.updateResource(j.id(), frame, currentTime);
    } else {
      throw new IllegalArgumentException("Unexpected subtype of %s: %s".formatted(JobId.class, job.getClass()));
    }
  }

  /** Perform the next step of a modeled task. */
  public void stepTask(
      final TaskId task,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final MissionModel<?> model
  ) {
    // The handler for each individual task stage is responsible
    //   for putting an updated lifecycle back into the task set.
    var lifecycle = this.tasks.remove(task);

    // Extract the current modeling state.
    if (lifecycle instanceof ExecutionState.IllegalSource) {
      // pass -- uninstantiable tasks never progress or complete
    } else if (lifecycle instanceof ExecutionState.NotStarted e) {
      stepEffectModel(task, e.startedAt(currentTime), frame, currentTime, model);
    } else if (lifecycle instanceof ExecutionState.InProgress e) {
      stepEffectModel(task, e, frame, currentTime, model);
    } else if (lifecycle instanceof ExecutionState.AwaitingChildren e) {
      stepWaitingTask(task, e, frame, currentTime);
    } else {
      // TODO: Log this issue to somewhere more general than stderr.
      System.err.println("Task %s is ready but in unexpected execution state %s".formatted(task, lifecycle));
    }
  }

  /** Make progress in a task by stepping its associated effect model forward. */
  private void stepEffectModel(
      final TaskId task,
      final ExecutionState.InProgress progress,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final MissionModel<?> model
  ) {
    // Step the modeling state forward.
    final var scheduler = new EngineScheduler(model, currentTime, task, frame);
    final var state = progress.state();
    final var status = state.step(scheduler);

    // TODO: Report which topics this activity wrote to at this point in time. This is useful insight for any user.
    // TODO: Report which cells this activity read from at this point in time. This is useful insight for any user.

    // Based on the task's return status, update its execution state and schedule its resumption.
    if (status instanceof TaskStatus.Completed completed) {
      final var children = new LinkedList<>(this.taskChildren.getOrDefault(task, Collections.emptySet()));

      this.tasks.put(task, progress.completedAt(currentTime, children));
      this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime));
      final var returnValue = completed.returnValue();
      if (returnValue.isPresent()) {
        this.taskReturns.put(task, (TaskReturnValue) returnValue.get());
      }
    } else if (status instanceof TaskStatus.Delayed s) {
      this.tasks.put(task, progress.continueWith(state));
      this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime.plus(s.delay())));
    } else if (status instanceof TaskStatus.AwaitingTask s) {
      this.tasks.put(task, progress.continueWith(state));

      final var target = (TaskId) s.target();
      final var targetExecution = this.tasks.get(target);
      if (targetExecution == null) {
        // TODO: Log that we saw a task ID that doesn't exist. Try to make this as visible as possible to users.
        // pass -- nonexistent tasks will never complete
      } else if (targetExecution instanceof ExecutionState.Terminated) {
        this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime));
      } else {
        this.waitingTasks.subscribeQuery(task, Set.of(SignalId.forTask((TaskId) s.target())));
      }
    } else if (status instanceof TaskStatus.AwaitingCondition s) {
      final var condition = ConditionId.generate();
      this.conditions.put(condition, s.condition());
      this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(currentTime));

      this.tasks.put(task, progress.continueWith(state));
      this.waitingTasks.subscribeQuery(task, Set.of(SignalId.forCondition(condition)));
    } else {
      throw new IllegalArgumentException("Unknown subclass of %s: %s".formatted(TaskStatus.class, status));
    }
  }

  /** Make progress in a task by checking if all of the tasks it's waiting on have completed. */
  private void stepWaitingTask(
      final TaskId task,
      final ExecutionState.AwaitingChildren awaiting,
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
      if (!(this.tasks.get(nextChild) instanceof ExecutionState.Terminated)) {
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
      final Duration horizonTime
  ) {
    final var querier = new EngineQuerier(frame);
    final var prediction = this.conditions
        .get(condition)
        .nextSatisfied(querier, horizonTime.minus(currentTime))
        .map(currentTime::plus);

    this.waitingConditions.subscribeQuery(condition, querier.referencedTopics);

    final var expiry = querier.expiry.map(currentTime::plus);
    if (prediction.isPresent() && (expiry.isEmpty() || prediction.get().shorterThan(expiry.get()))) {
      this.scheduledJobs.schedule(JobId.forSignal(SignalId.forCondition(condition)), SubInstant.Tasks.at(prediction.get()));
    } else {
      // Try checking again later -- where "later" is in some non-zero amount of time!
      final var nextCheckTime = Duration.max(expiry.orElse(horizonTime), currentTime.plus(Duration.EPSILON));
      this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(nextCheckTime));
    }
  }

  /** Get the current behavior of a given resource and accumulate it into the resource's profile. */
  public void updateResource(
      final ResourceId resource,
      final TaskFrame<JobId> frame,
      final Duration currentTime
  ) {
    final var querier = new EngineQuerier(frame);
    this.resources.get(resource).append(currentTime, querier);

    this.waitingResources.subscribeQuery(resource, querier.referencedTopics);

    final var expiry = querier.expiry.map(currentTime::plus);
    if (expiry.isPresent()) {
      this.scheduledJobs.schedule(JobId.forResource(resource), SubInstant.Resources.at(expiry.get()));
    }
  }

  /** Resets all tasks (freeing any held resources). The engine should not be used after being closed. */
  @Override
  public void close() {
    for (final var task : this.tasks.values()) {
      if (task instanceof ExecutionState.InProgress r) {
        r.state.reset();
      }
    }
  }

  /** Determine if a given task has fully completed. */
  public boolean isTaskComplete(final TaskId task) {
    return (this.tasks.get(task) instanceof ExecutionState.Terminated);
  }

  /** Compute a set of results from the current state of simulation. */
  // TODO: Move result extraction out of the SimulationEngine.
  //   The Engine should only need to stream events of interest to a downstream consumer.
  //   The Engine cannot be cognizant of all downstream needs.
  // TODO: Whatever mechanism replaces `computeResults` also ought to replace `isTaskComplete`.
  // TODO: Produce results for all tasks, not just those that have completed.
  //   Planners need to be aware of failed or unfinished tasks.
  public SimulationResults computeResults(
      final SimulationEngine engine,
      final Instant startTime,
      final Duration elapsedTime,
      final Map<String, String> taskToPlannedDirective,
      final TemporalEventSource timeline,
      final MissionModel<?> missionModel) {
    final var realProfiles = new HashMap<String, List<Pair<Duration, RealDynamics>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>();

    for (final var entry : engine.resources.entrySet()) {
      final var id = entry.getKey();
      final var state = entry.getValue();

      final var name = id.id();
      final var resource = state.resource();

      switch (resource.getType()) {
        case "real" -> realProfiles.put(
            name,
            serializeProfile(elapsedTime, state, SimulationEngine::extractRealDynamics));

        case "discrete" -> discreteProfiles.put(
            name,
            Pair.of(
                state.resource().getSchema(),
                serializeProfile(elapsedTime, state, Resource::serialize)));

        default ->
            throw new IllegalArgumentException(
                "Resource `%s` has unknown type `%s`".formatted(name, resource.getType()));
      }
    }

    engine.tasks.forEach((task, state) -> {
      final var directive = engine.taskDirective.get(task);
      if (directive == null) return;

      taskToPlannedDirective.computeIfAbsent(task.id(), id -> id);
    });

    final var activityParents = new HashMap<String, String>();
    engine.tasks.forEach((task, state) -> {
      final var directive = engine.taskDirective.get(task);
      if (directive == null) return;

      var parent = engine.taskParent.get(task);
      while (parent != null && !engine.taskDirective.containsKey(parent)) {
        parent = engine.taskParent.get(parent);
      }

      if (parent != null) {
        activityParents.put(taskToPlannedDirective.get(task.id()), taskToPlannedDirective.get(parent.id()));
      }
    });

    final var activityChildren = new HashMap<String, List<String>>();
    activityParents.forEach((task, parent) -> {
      activityChildren.computeIfAbsent(parent, $ -> new LinkedList<>()).add(task);
    });

    final var simulatedActivities = new HashMap<String, SimulatedActivity>();
    final var unsimulatedActivities = new HashMap<String, SerializedActivity>();
    engine.tasks.forEach((task, state) -> {
      final var directive = engine.taskDirective.get(task);
      if (directive == null) return;

      final var activityId = taskToPlannedDirective.get(task.id());

      if (state instanceof ExecutionState.Terminated e) {
        simulatedActivities.put(activityId, new SimulatedActivity(
            directive.getType(),
            directive.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            e.joinOffset().minus(e.startOffset()),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.of(activityId)
        ));
      } else {
        unsimulatedActivities.put(activityId, new SerializedActivity(
            directive.getType(),
            directive.getArguments()));
      }
    });

    final List<Triple<Integer, String, ValueSchema>> topics = new ArrayList<>();
    final var serializableTopicToId = new HashMap<MissionModel.SerializableTopic<?>, Integer>();
    for (final var serializableTopic : missionModel.getTopics()) {
      serializableTopicToId.put(serializableTopic, topics.size());
      topics.add(Triple.of(topics.size(), serializableTopic.name(), serializableTopic.valueSchema()));
    }

    final var serializedTimeline = new TreeMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>>();
    var time = Duration.ZERO;
    for (var point : timeline.points()) {
      if (point instanceof TemporalEventSource.TimePoint.Delta delta) {
        time = time.plus(delta.delta());
      } else if (point instanceof TemporalEventSource.TimePoint.Commit commit) {
        final var serializedEventGraph = commit.events().substitute(
            event -> {
              EventGraph<Pair<Integer, SerializedValue>> output = EventGraph.empty();
              for (final var serializableTopic : missionModel.getTopics()) {
                Optional<SerializedValue> serializedEvent = trySerializeEvent(event, serializableTopic);
                if (serializedEvent.isPresent()) {
                  output = EventGraph.concurrently(output, EventGraph.atom(Pair.of(serializableTopicToId.get(serializableTopic), serializedEvent.get())));
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

    return new SimulationResults(realProfiles,
                                 discreteProfiles,
                                 simulatedActivities,
                                 unsimulatedActivities,
                                 startTime,
                                 topics,
                                 serializedTimeline);
  }

  private <EventType> Optional<SerializedValue> trySerializeEvent(Event event, MissionModel.SerializableTopic<EventType> serializableTopic) {
    return event.extract(topicOfSerializableTopic(serializableTopic), serializableTopic.serializer());
  }

  private <EventType> Topic<EventType> topicOfSerializableTopic(MissionModel.SerializableTopic<EventType> serializableTopic) {
    // SAFETY: All queries available to the model are given to it by the MissionModelBuilder, which always constructs EngineQuery instances.
    return ((EngineQuery<EventType, ?>) serializableTopic.query()).topic();
  }

  private interface Translator<Target> {
    <Dynamics> Target apply(Resource<Dynamics> resource, Dynamics dynamics);
  }

  private static <Target, Dynamics>
  List<Pair<Duration, Target>> serializeProfile(
      final Duration elapsedTime,
      final ProfilingState<Dynamics> state,
      final Translator<Target> translator
  ) {
    final var profile = new ArrayList<Pair<Duration, Target>>(state.profile().segments().size());

    final var iter = state.profile().segments().iterator();
    if (iter.hasNext()) {
      var segment = iter.next();
      while (iter.hasNext()) {
        final var nextSegment = iter.next();

        profile.add(Pair.of(
            nextSegment.startOffset().minus(segment.startOffset()),
            translator.apply(state.resource(), segment.dynamics())));
        segment = nextSegment;
      }

      profile.add(Pair.of(
          elapsedTime.minus(segment.startOffset()),
          translator.apply(state.resource(), segment.dynamics())));
    }

    return profile;
  }

  private static <Dynamics>
  RealDynamics extractRealDynamics(final Resource<Dynamics> resource, final Dynamics dynamics) {
    final var serializedSegment = resource.serialize(dynamics).asMap().orElseThrow();
    final var initial = serializedSegment.get("initial").asReal().orElseThrow();
    final var rate = serializedSegment.get("rate").asReal().orElseThrow();

    return RealDynamics.linear(initial, rate);
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
    public <State> State getState(final Query<?, State> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via MissionModelBuilder).
      @SuppressWarnings("unchecked")
      final var query = ((EngineQuery<?, State>) token);

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
  private final class EngineScheduler implements Scheduler {
    private final MissionModel<?> model;

    private final Duration currentTime;
    private final TaskId activeTask;
    private final TaskFrame<JobId> frame;

    public EngineScheduler(
        final MissionModel<?> model,
        final Duration currentTime,
        final TaskId activeTask,
        final TaskFrame<JobId> frame
    ) {
      this.model = Objects.requireNonNull(model);
      this.currentTime = Objects.requireNonNull(currentTime);
      this.activeTask = Objects.requireNonNull(activeTask);
      this.frame = Objects.requireNonNull(frame);
    }

    @Override
    public <State> State get(final Query<?, State> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via MissionModelBuilder).
      @SuppressWarnings("unchecked")
      final var query = ((EngineQuery<?, State>) token);

      // TODO: Cache the return value (until the next emit or until the task yields) to avoid unnecessary copies
      //  if the same state is requested multiple times in a row.
      final var state$ = this.frame.getState(query.query());
      return state$.orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public <EventType> void emit(final EventType event, final Query<? super EventType, ?> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via MissionModelBuilder).
      @SuppressWarnings("unchecked")
      final var topic = ((EngineQuery<? super EventType, ?>) token).topic();

      // Append this event to the timeline.
      this.frame.emit(Event.create(topic, event));

      SimulationEngine.this.invalidateTopic(topic, this.currentTime);
    }

    @Override
    public TaskIdentifier spawn(final Task state) {
      final var task = TaskId.generate();
      SimulationEngine.this.tasks.put(task, new ExecutionState.InProgress(this.currentTime, state));
      SimulationEngine.this.taskParent.put(task, this.activeTask);
      SimulationEngine.this.taskChildren.computeIfAbsent(this.activeTask, $ -> new HashSet<>()).add(task);
      this.frame.signal(JobId.forTask(task));

      return task;
    }

    @Override
    public TaskIdentifier spawn(final String type, final Map<String, SerializedValue> arguments) {
      final var task = initiateTaskFromInput(this.model, new SerializedActivity(type, arguments));
      SimulationEngine.this.taskParent.put(task, this.activeTask);
      SimulationEngine.this.taskChildren.computeIfAbsent(this.activeTask, $ -> new HashSet<>()).add(task);
      this.frame.signal(JobId.forTask(task));

      return task;
    }

    @Override
    public TaskIdentifier defer(final Duration delay, final Task state) {
      if (delay.isNegative()) throw new IllegalArgumentException("Cannot schedule a task in the past");

      final var task = TaskId.generate();
      SimulationEngine.this.tasks.put(task, new ExecutionState.InProgress(this.currentTime, state));
      SimulationEngine.this.taskParent.put(task, this.activeTask);
      SimulationEngine.this.taskChildren.computeIfAbsent(this.activeTask, $ -> new HashSet<>()).add(task);
      SimulationEngine.this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(this.currentTime.plus(delay)));

      return task;
    }

    @Override
    public TaskIdentifier defer(final Duration delay, final String type, final Map<String, SerializedValue> arguments) {
      if (delay.isNegative()) throw new IllegalArgumentException("Cannot schedule a task in the past");

      final var task = initiateTaskFromInput(this.model, new SerializedActivity(type, arguments));
      SimulationEngine.this.taskParent.put(task, this.activeTask);
      SimulationEngine.this.taskChildren.computeIfAbsent(this.activeTask, $ -> new HashSet<>()).add(task);
      SimulationEngine.this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(this.currentTime.plus(delay)));

      return task;
    }

    @Override
    public Object getTaskReturnValue(final TaskIdentifier taskId) {
      final var taskReturnValue = SimulationEngine.this.taskReturns.get(((TaskId) taskId));
      if (taskReturnValue == null) return null;
      return taskReturnValue.returnValue();
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
  private sealed interface ExecutionState {
    /** The task has an invalid source for its behavior. */
    // TODO: Provide more details about the instantiation failure.
    record IllegalSource()
        implements ExecutionState {}

    /** The task has not yet started. */
    record NotStarted(TaskSource source)
        implements ExecutionState
    {
      public InProgress startedAt(final Duration startOffset) {
        return new InProgress(startOffset, this.source.createTask());
      }
    }

    /** The task is in its primary operational phase. */
    record InProgress(Duration startOffset, Task state)
        implements ExecutionState
    {
      public AwaitingChildren completedAt(final Duration endOffset, final LinkedList<TaskId> remainingChildren) {
        return new AwaitingChildren(this.startOffset, endOffset, remainingChildren);
      }

      public InProgress continueWith(final Task newState) {
        return new InProgress(this.startOffset, newState);
      }
    }

    /** The task has completed its primary operation, but has unfinished children. */
    record AwaitingChildren(Duration startOffset, Duration endOffset, LinkedList<TaskId> remainingChildren)
        implements ExecutionState
    {
      public Terminated joinedAt(final Duration joinOffset) {
        return new Terminated(this.startOffset, this.endOffset, joinOffset);
      }
    }

    /** The task and all its delegated children have completed. */
    record Terminated(Duration startOffset, Duration endOffset, Duration joinOffset)
        implements ExecutionState {}
  }
}
