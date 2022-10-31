package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel.SerializableTopic;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
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
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.tuple.Pair;

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
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
  private final Map<TaskId, ExecutionState<?>> tasks = new HashMap<>();
  /** The getter for each tracked condition. */
  private final Map<ConditionId, Condition> conditions = new HashMap<>();
  /** The profiling state for each tracked resource. */
  private final Map<ResourceId, ResourceAndListener<?>> resources = new HashMap<>();

  /** The task that spawned a given task (if any). */
  private final Map<TaskId, TaskId> taskParent = new HashMap<>();
  /** The set of children for each task (if any). */
  @DerivedFrom("taskParent")
  private final Map<TaskId, Set<TaskId>> taskChildren = new HashMap<>();

  /** Schedule a new task to be performed at the given time. */
  public <Return> TaskId scheduleTask(final Duration startTime, final Task<Return> state) {
    final var task = TaskId.generate();
    this.tasks.put(task, new ExecutionState.InProgress<>(startTime, state));
    this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(startTime));
    return task;
  }

  public interface ResourceListener<Dynamics> {
    void update(Duration currentTime, Dynamics dynamics);
  }

  record ResourceAndListener<Dynamics>(
      Resource<Dynamics> resource,
      ResourceListener<Dynamics> listener
  ) {}

  /** Register a resource whose profile should be accumulated over time. */
  public <Dynamics>
  void trackResource(final String name, Resource<Dynamics> resource, final Duration nextQueryTime, final ResourceListener<Dynamics> listener) {
    final var id = new ResourceId(name);

    this.resources.put(id, new ResourceAndListener<>(resource, listener));
    this.scheduledJobs.schedule(JobId.forResource(id), SubInstant.Resources.at(nextQueryTime));
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
      final Duration maximumTime
  ) {
    var tip = EventGraph.<Event>empty();
    for (final var job$ : jobs) {
      tip = EventGraph.concurrently(tip, TaskFrame.run(job$, context, (job, frame) -> {
        this.performJob(job, frame, currentTime, maximumTime);
      }));
    }

    return tip;
  }

  /** Performs a single job. */
  public void performJob(
      final JobId job,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final Duration maximumTime
  ) {
    if (job instanceof JobId.TaskJobId j) {
      this.stepTask(j.id(), frame, currentTime);
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
  public void stepTask(final TaskId task, final TaskFrame<JobId> frame, final Duration currentTime) {
    // The handler for each individual task stage is responsible
    //   for putting an updated lifecycle back into the task set.
    var lifecycle = this.tasks.remove(task);

    stepTaskHelper(task, frame, currentTime, lifecycle);
  }

  private <Return> void stepTaskHelper(
      final TaskId task,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final ExecutionState<Return> lifecycle)
  {
    // Extract the current modeling state.
    if (lifecycle instanceof ExecutionState.InProgress<Return> e) {
      stepEffectModel(task, e, frame, currentTime);
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
      final Duration currentTime
  ) {
    // Step the modeling state forward.
    final var scheduler = new EngineScheduler(currentTime, task, frame);
    final var status = progress.state().step(scheduler);

    // TODO: Report which topics this activity wrote to at this point in time. This is useful insight for any user.
    // TODO: Report which cells this activity read from at this point in time. This is useful insight for any user.

    // Based on the task's return status, update its execution state and schedule its resumption.
    if (status instanceof TaskStatus.Completed<Return>) {
      final var children = new LinkedList<>(this.taskChildren.getOrDefault(task, Collections.emptySet()));

      this.tasks.put(task, progress.completedAt(currentTime, children));
      this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime));
    } else if (status instanceof TaskStatus.Delayed<Return> s) {
      this.tasks.put(task, progress.continueWith(s.continuation()));
      this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime.plus(s.delay())));
    } else if (status instanceof TaskStatus.CallingTask<Return> s) {
      final var target = TaskId.generate();
      SimulationEngine.this.tasks.put(target, new ExecutionState.InProgress<>(currentTime, s.child()));
      SimulationEngine.this.taskParent.put(target, task);
      SimulationEngine.this.taskChildren.computeIfAbsent(task, $ -> new HashSet<>()).add(target);
      frame.signal(JobId.forTask(target));

      this.tasks.put(task, progress.continueWith(s.continuation()));

      final var targetExecution = this.tasks.get(target);
      if (targetExecution == null) {
        // TODO: Log that we saw a task ID that doesn't exist. Try to make this as visible as possible to users.
        // pass -- nonexistent tasks will never complete
      } else if (targetExecution instanceof ExecutionState.Terminated) {
        this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime));
      } else {
        this.waitingTasks.subscribeQuery(task, Set.of(SignalId.forTask(target)));
      }
    } else if (status instanceof TaskStatus.AwaitingCondition<Return> s) {
      final var condition = ConditionId.generate();
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
    helper(currentTime, querier, this.resources.get(resource));

    this.waitingResources.subscribeQuery(resource, querier.referencedTopics);

    final var expiry = querier.expiry.map(currentTime::plus);
    if (expiry.isPresent()) {
      this.scheduledJobs.schedule(JobId.forResource(resource), SubInstant.Resources.at(expiry.get()));
    }
  }

  private <Dynamics> void helper(Duration currentTime, EngineQuerier querier, ResourceAndListener<Dynamics> resourceAndListener) {
    resourceAndListener.listener.update(currentTime, resourceAndListener.resource.getDynamics(querier));
  }

  /** Resets all tasks (freeing any held resources). The engine should not be used after being closed. */
  @Override
  public void close() {
    for (final var task : this.tasks.values()) {
      if (task instanceof ExecutionState.InProgress r) {
        r.state.release();
      }
    }
  }

  /** Determine if a given task has fully completed. */
  public boolean isTaskComplete(final TaskId task) {
    return (this.tasks.get(task) instanceof ExecutionState.Terminated);
  }

  private record TaskInfo(
      Map<String, ActivityInstanceId> taskToPlannedDirective,
      Map<String, SerializedActivity> input,
      Map<String, SerializedValue> output
  ) {
    public TaskInfo() {
      this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public boolean isActivity(final TaskId id) {
      return this.input.containsKey(id.id());
    }

    public record Trait(Iterable<SerializableTopic<?>> topics, Topic<ActivityInstanceId> activityTopic) implements EffectTrait<Consumer<TaskInfo>> {
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
            .ifPresent(directiveId -> taskInfo.taskToPlannedDirective.put(ev.provenance().id(), directiveId));

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

  public static class ResourceProfilePair<Dynamics> {
    public final Resource<Dynamics> resource;
    private Profile<Dynamics> profile;

    public ResourceProfilePair(final Resource<Dynamics> resource) {
      this.resource = resource;
      this.profile = new Profile<>();
    }

    public Profile<Dynamics> profile() {
      return this.profile;
    }

    public void clear() {
      this.profile = new Profile<>();
    }
  }

  /** Compute a set of results from the current state of simulation. */
  // TODO: Move result extraction out of the SimulationEngine.
  //   The Engine should only need to stream events of interest to a downstream consumer.
  //   The Engine cannot be cognizant of all downstream needs.
  // TODO: Whatever mechanism replaces `computeResults` also ought to replace `isTaskComplete`.
  // TODO: Produce results for all tasks, not just those that have completed.
  //   Planners need to be aware of failed or unfinished tasks.
  public static Pair<HashMap<ActivityInstanceId, SimulatedActivity>, HashMap<ActivityInstanceId, UnfinishedActivity>> extractActivityInfo(
      final SimulationEngine engine,
      final Instant startTime,
      final Topic<ActivityInstanceId> activityTopic,
      final TemporalEventSource timeline,
      final Iterable<SerializableTopic<?>> serializableTopics
  ) {
    // Collect per-task information from the event graph.
    final var taskInfo = new TaskInfo();

    for (final var point : timeline) {
      if (!(point instanceof TemporalEventSource.TimePoint.Commit p)) continue;

      final var trait = new TaskInfo.Trait(serializableTopics, activityTopic);
      p.events().evaluate(trait, trait::atom).accept(taskInfo);
    }

    // Give every task corresponding to a child activity an ID that doesn't conflict with any root activity.
    final var taskToPlannedDirective = new HashMap<>(taskInfo.taskToPlannedDirective);
    final var usedActivityInstanceIds =
        taskToPlannedDirective
            .values()
            .stream()
            .map(ActivityInstanceId::id)
            .collect(Collectors.toSet());
    var counter = 1L;
    for (final var task : engine.tasks.keySet()) {
      if (!taskInfo.isActivity(task)) continue;
      if (taskToPlannedDirective.containsKey(task.id())) continue;

      while (usedActivityInstanceIds.contains(counter)) counter++;
      taskToPlannedDirective.put(task.id(), new ActivityInstanceId(counter++));
    }

    // Identify the nearest ancestor *activity* (excluding intermediate anonymous tasks).
    final var activityParents = new HashMap<ActivityInstanceId, ActivityInstanceId>();
    engine.tasks.forEach((task, state) -> {
      if (!taskInfo.isActivity(task)) return;

      var parent = engine.taskParent.get(task);
      while (parent != null && !taskInfo.isActivity(parent)) {
        parent = engine.taskParent.get(parent);
      }

      if (parent != null) {
        activityParents.put(taskToPlannedDirective.get(task.id()), taskToPlannedDirective.get(parent.id()));
      }
    });

    final var activityChildren = new HashMap<ActivityInstanceId, List<ActivityInstanceId>>();
    activityParents.forEach((task, parent) -> {
      activityChildren.computeIfAbsent(parent, $ -> new LinkedList<>()).add(task);
    });

    final var simulatedActivities = new HashMap<ActivityInstanceId, SimulatedActivity>();
    final var unfinishedActivities = new HashMap<ActivityInstanceId, UnfinishedActivity>();
    engine.tasks.forEach((task, state) -> {
      if (!taskInfo.isActivity(task)) return;

      final var activityId = taskToPlannedDirective.get(task.id());

      if (state instanceof ExecutionState.Terminated<?> e) {
        final var inputAttributes = taskInfo.input().get(task.id());
        final var outputAttributes = taskInfo.output().get(task.id());

        simulatedActivities.put(activityId, new SimulatedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            e.joinOffset().minus(e.startOffset()),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.of(activityId),
            outputAttributes
        ));
      } else if (state instanceof ExecutionState.InProgress<?> e){
        final var inputAttributes = taskInfo.input().get(task.id());
        unfinishedActivities.put(activityId, new UnfinishedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.of(activityId)
        ));
      } else if (state instanceof ExecutionState.AwaitingChildren<?> e){
        final var inputAttributes = taskInfo.input().get(task.id());
        unfinishedActivities.put(activityId, new UnfinishedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.of(activityId)
        ));
      } else {
        throw new Error("Unexpected subtype of %s: %s".formatted(ExecutionState.class, state.getClass()));
      }
    });

    return Pair.of(simulatedActivities, unfinishedActivities);
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
  List<Pair<Duration, Target>> serializeProfile(
      final Duration elapsedTime,
      final ResourceProfilePair<Dynamics> pair,
      final Translator<Target> translator
  ) {
    final var profile = new ArrayList<Pair<Duration, Target>>(pair.profile().segments().size());

    final var iter = pair.profile().segments().iterator();
    if (iter.hasNext()) {
      var segment = iter.next();
      while (iter.hasNext()) {
        final var nextSegment = iter.next();

        profile.add(Pair.of(
            nextSegment.startOffset().minus(segment.startOffset()),
            translator.apply(pair.resource, segment.dynamics())));
        segment = nextSegment;
      }

      profile.add(Pair.of(
          elapsedTime.minus(segment.startOffset()),
          translator.apply(pair.resource, segment.dynamics())));
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
  private final class EngineScheduler implements Scheduler {
    private final Duration currentTime;
    private final TaskId activeTask;
    private final TaskFrame<JobId> frame;

    public EngineScheduler(final Duration currentTime, final TaskId activeTask, final TaskFrame<JobId> frame) {
      this.currentTime = Objects.requireNonNull(currentTime);
      this.activeTask = Objects.requireNonNull(activeTask);
      this.frame = Objects.requireNonNull(frame);
    }

    @Override
    public <State> State get(final CellId<State> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via MissionModelBuilder).
      @SuppressWarnings("unchecked")
      final var query = ((EngineCellId<?, State>) token);

      // TODO: Cache the return value (until the next emit or until the task yields) to avoid unnecessary copies
      //  if the same state is requested multiple times in a row.
      final var state$ = this.frame.getState(query.query());
      return state$.orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public <EventType> void emit(final EventType event, final Topic<EventType> topic) {
      // Append this event to the timeline.
      this.frame.emit(Event.create(topic, event, this.activeTask));

      SimulationEngine.this.invalidateTopic(topic, this.currentTime);
    }

    @Override
    public void spawn(final Task<?> state) {
      final var task = TaskId.generate();
      SimulationEngine.this.tasks.put(task, new ExecutionState.InProgress<>(this.currentTime, state));
      SimulationEngine.this.taskParent.put(task, this.activeTask);
      SimulationEngine.this.taskChildren.computeIfAbsent(this.activeTask, $ -> new HashSet<>()).add(task);
      this.frame.signal(JobId.forTask(task));
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
