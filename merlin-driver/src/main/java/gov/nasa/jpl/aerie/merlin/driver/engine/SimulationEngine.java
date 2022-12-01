package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel.SerializableTopic;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivityId;
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
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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
  private final Map<ResourceId, ProfilingState<?>> resources = new HashMap<>();

  /** The task that spawned a given task (if any). */
  private final Map<TaskId, TaskId> taskParent = new HashMap<>();
  /** The set of children for each task (if any). */
  @DerivedFrom("taskParent")
  private final Map<TaskId, Set<TaskId>> taskChildren = new HashMap<>();

  /** A thread pool that modeled tasks can use to keep track of their state between steps. */
  private final ExecutorService executor = getLoomOrFallback();

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
  public <Output> TaskId scheduleTask(final Duration startTime, final TaskFactory<Unit, Output> state) {
    if (startTime.isNegative()) throw new IllegalArgumentException("Cannot schedule a task before the start time of the simulation");

    final var task = TaskId.generate();
    this.tasks.put(task, new ExecutionState.InProgress<>(
        startTime,
        state.create(this.executor),
        new MutableObject<>(Unit.UNIT),
        $ -> {}));
    this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(startTime));
    return task;
  }

  /** Register a resource whose profile should be accumulated over time. */
  public <Dynamics>
  void trackResource(final String name, final Resource<Dynamics> resource, final Duration nextQueryTime) {
    final var id = new ResourceId(name);

    this.resources.put(id, ProfilingState.create(resource));
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

  private <Output> void stepTaskHelper(
      final TaskId task,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final ExecutionState<Output> lifecycle)
  {
    // Extract the current modeling state.
    if (lifecycle instanceof ExecutionState.InProgress<?, Output> e) {
      stepEffectModel(task, e, frame, currentTime);
    } else if (lifecycle instanceof ExecutionState.AwaitingChildren<Output> e) {
      stepWaitingTask(task, e, frame, currentTime);
    } else {
      // TODO: Log this issue to somewhere more general than stderr.
      System.err.println("Task %s is ready but in unexpected execution state %s".formatted(task, lifecycle));
    }
  }

  /** Make progress in a task by stepping its associated effect model forward. */
  private <Input, Output> void stepEffectModel(
      final TaskId task,
      final ExecutionState.InProgress<Input, Output> progress,
      final TaskFrame<JobId> frame,
      final Duration currentTime
  ) {
    // Step the modeling state forward.
    final var scheduler = new EngineScheduler(currentTime, task, frame);
    final var status = progress.state().step(scheduler, progress.input().getValue());

    // TODO: Report which topics this activity wrote to at this point in time. This is useful insight for any user.
    // TODO: Report which cells this activity read from at this point in time. This is useful insight for any user.

    // Based on the task's return status, update its execution state and schedule its resumption.
    if (status instanceof TaskStatus.Completed<Output> s) {
      // Propagate this task's output to any interested parties.
      progress.output().accept(s.returnValue());

      final var children = new LinkedList<>(this.taskChildren.getOrDefault(task, Collections.emptySet()));

      this.tasks.put(task, progress.completedAt(currentTime, children));
      this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime));
    } else if (status instanceof TaskStatus.Delayed<Output> s) {
      if (s.delay().isNegative()) throw new IllegalArgumentException("Cannot schedule a task in the past");

      this.tasks.put(task, progress.continueWith(s.continuation(), Unit.UNIT));
      this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime.plus(s.delay())));
    } else if (status instanceof TaskStatus.CallingTask<?, Output> s) {
      processCallStatus(task, progress, frame, currentTime, s);
    } else if (status instanceof TaskStatus.AwaitingCondition<Output> s) {
      final var condition = ConditionId.generate();
      this.conditions.put(condition, s.condition());
      this.scheduledJobs.schedule(JobId.forCondition(condition), SubInstant.Conditions.at(currentTime));

      this.tasks.put(task, progress.continueWith(s.continuation(), Unit.UNIT));
      this.waitingTasks.subscribeQuery(task, Set.of(SignalId.forCondition(condition)));
    } else {
      throw new IllegalArgumentException("Unknown subclass of %s: %s".formatted(TaskStatus.class, status));
    }
  }

  // This helper binds Midput; if it were inlined, the captured `?` would not propagate correctly,
  // causing type errors.
  private <Midput, Output>
  void processCallStatus(
      final TaskId task,
      final ExecutionState.InProgress<?, Output> progress,
      final TaskFrame<JobId> frame,
      final Duration currentTime,
      final TaskStatus.CallingTask<Midput, Output> s
  ) {
    final var child = TaskId.generate();

    // Waiting for Midput; will produce Output
    final var blocked = progress.blockWith(s.continuation());
    this.tasks.put(task, blocked);
    this.waitingTasks.subscribeQuery(task, Set.of(SignalId.forTask(child)));

    // Ready to run (with the given Input); will produce Midput
    this.tasks.put(child, new ExecutionState.InProgress<>(
        currentTime,
        s.child().create(this.executor),
        new MutableObject<>(Unit.UNIT),
        blocked.input()::setValue));
    this.taskParent.put(child, task);
    this.taskChildren.computeIfAbsent(task, $ -> new HashSet<>()).add(child);

    frame.signal(JobId.forTask(child));
  }

  /** Make progress in a task by checking if all of the tasks it's waiting on have completed. */
  private <Output> void stepWaitingTask(
      final TaskId task,
      final ExecutionState.AwaitingChildren<Output> awaiting,
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
      if (task instanceof ExecutionState.InProgress<?, ?> r) {
        r.state.release();
      }
    }

    this.executor.shutdownNow();
  }

  /** Determine if a given task has fully completed. */
  public boolean isTaskComplete(final TaskId task) {
    return (this.tasks.get(task) instanceof ExecutionState.Terminated);
  }

  private record TaskInfo(
      Map<String, ActivityDirectiveId> taskToPlannedDirective,
      Map<String, SerializedActivity> input,
      Map<String, SerializedValue> output
  ) {
    public TaskInfo() {
      this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public boolean isActivity(final TaskId id) {
      return this.input.containsKey(id.id());
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

  /** Compute a set of results from the current state of simulation. */
  // TODO: Move result extraction out of the SimulationEngine.
  //   The Engine should only need to stream events of interest to a downstream consumer.
  //   The Engine cannot be cognizant of all downstream needs.
  // TODO: Whatever mechanism replaces `computeResults` also ought to replace `isTaskComplete`.
  // TODO: Produce results for all tasks, not just those that have completed.
  //   Planners need to be aware of failed or unfinished tasks.
  public static SimulationResults computeResults(
      final SimulationEngine engine,
      final Instant startTime,
      final Duration elapsedTime,
      final Topic<ActivityDirectiveId> activityTopic,
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

    // Extract profiles for every resource.
    final var realProfiles = new HashMap<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>>();

    for (final var entry : engine.resources.entrySet()) {
      final var id = entry.getKey();
      final var state = entry.getValue();

      final var name = id.id();
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
    for (final var task : engine.tasks.keySet()) {
      if (!taskInfo.isActivity(task)) continue;
      if (taskToSimulatedActivityId.containsKey(task.id())) continue;

      while (usedSimulatedActivityIds.contains(counter)) counter++;
      taskToSimulatedActivityId.put(task.id(), new SimulatedActivityId(counter++));
    }

    // Identify the nearest ancestor *activity* (excluding intermediate anonymous tasks).
    final var activityParents = new HashMap<SimulatedActivityId, SimulatedActivityId>();
    engine.tasks.forEach((task, state) -> {
      if (!taskInfo.isActivity(task)) return;

      var parent = engine.taskParent.get(task);
      while (parent != null && !taskInfo.isActivity(parent)) {
        parent = engine.taskParent.get(parent);
      }

      if (parent != null) {
        activityParents.put(taskToSimulatedActivityId.get(task.id()), taskToSimulatedActivityId.get(parent.id()));
      }
    });

    final var activityChildren = new HashMap<SimulatedActivityId, List<SimulatedActivityId>>();
    activityParents.forEach((task, parent) -> {
      activityChildren.computeIfAbsent(parent, $ -> new LinkedList<>()).add(task);
    });

    final var simulatedActivities = new HashMap<SimulatedActivityId, SimulatedActivity>();
    final var unfinishedActivities = new HashMap<SimulatedActivityId, UnfinishedActivity>();
    engine.tasks.forEach((task, state) -> {
      if (!taskInfo.isActivity(task)) return;

      final var activityId = taskToSimulatedActivityId.get(task.id());
      final var directiveId = taskInfo.taskToPlannedDirective.get(task.id()); // will be null for non-directives

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
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.of(directiveId),
            outputAttributes
        ));
      } else if (state instanceof ExecutionState.InProgress<?, ?> e){
        final var inputAttributes = taskInfo.input().get(task.id());
        unfinishedActivities.put(activityId, new UnfinishedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.of(directiveId)
        ));
      } else if (state instanceof ExecutionState.AwaitingChildren<?> e){
        final var inputAttributes = taskInfo.input().get(task.id());
        unfinishedActivities.put(activityId, new UnfinishedActivity(
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

    final List<Triple<Integer, String, ValueSchema>> topics = new ArrayList<>();
    final var serializableTopicToId = new HashMap<SerializableTopic<?>, Integer>();
    for (final var serializableTopic : serializableTopics) {
      serializableTopicToId.put(serializableTopic, topics.size());
      topics.add(Triple.of(topics.size(), serializableTopic.name(), serializableTopic.outputType().getSchema()));
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
          serializedTimeline
              .computeIfAbsent(time, x -> new ArrayList<>())
              .add(serializedEventGraph);
        }
      }
    }

    return new SimulationResults(realProfiles,
                                 discreteProfiles,
                                 simulatedActivities,
                                 unfinishedActivities,
                                 startTime,
                                 topics,
                                 serializedTimeline);
  }

  public Optional<Duration> getTaskDuration(TaskId taskId){
    final var state = tasks.get(taskId);
    if (state instanceof ExecutionState.Terminated<?> e) {
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
    public void spawn(final TaskFactory<Unit, ?> state) {
      final var task = TaskId.generate();
      SimulationEngine.this.tasks.put(task, new ExecutionState.InProgress<>(
          this.currentTime,
          state.create(SimulationEngine.this.executor),
          new MutableObject<>(Unit.UNIT),
          $ -> {}));
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
  private sealed interface ExecutionState<Output> {
    record InProgress<Input, Output>(
        Duration startOffset,
        Task<Input, Output> state,
        MutableObject<Input> input,
        Consumer<Output> output
    ) implements ExecutionState<Output>
    {
      public AwaitingChildren<Output> completedAt(final Duration endOffset, final LinkedList<TaskId> remainingChildren) {
        return new AwaitingChildren<>(this.startOffset, endOffset, remainingChildren);
      }

      public <Midput> InProgress<Midput, Output> continueWith(final Task<Midput, Output> newState, final Midput midput) {
        return new InProgress<>(this.startOffset, newState, new MutableObject<>(midput), this.output);
      }

      public <Midput> InProgress<Midput, Output> blockWith(final Task<Midput, Output> newState) {
        return new InProgress<>(this.startOffset, newState, new MutableObject<>(), this.output);
      }
    }

    /** The task has completed its primary operation, but has delegated work to children which may not be finished. */
    record AwaitingChildren<Output>(
        Duration startOffset,
        Duration endOffset,
        LinkedList<TaskId> remainingChildren
    ) implements ExecutionState<Output>
    {
      public Terminated<Output> joinedAt(final Duration joinOffset) {
        return new Terminated<>(this.startOffset, this.endOffset, joinOffset);
      }
    }

    /** The task and all its delegated children have completed. */
    record Terminated<Output>(
        Duration startOffset,
        Duration endOffset,
        Duration joinOffset
    ) implements ExecutionState<Output> {}
  }
}
