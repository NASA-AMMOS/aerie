package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Approximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.RealApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.timeline.History;
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
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

/**
 * A representation of the work remaining to do during a simulation, and its accumulated results.
 *
 * @param <$Timeline> The brand associated with the simulation timeline to which state-altering events are recorded.
 */
public final class SimulationEngine<$Timeline> implements AutoCloseable {
  /** The set of all jobs waiting for time to pass. */
  private final JobSchedule<JobId, SchedulingInstant> scheduledJobs = new JobSchedule<>();
  /** The set of all jobs waiting on a given signal. */
  private final Subscriptions<SignalId, TaskId> waitingTasks = new Subscriptions<>();
  /** The set of conditions depending on a given set of topics. */
  private final Subscriptions<TopicId, ConditionId> waitingConditions = new Subscriptions<>();
  /** The set of queries depending on a given set of topics. */
  private final Subscriptions<TopicId, ResourceId> waitingResources = new Subscriptions<>();

  /** The execution state for every task. */
  private final Map<TaskId, ExecutionState<$Timeline>> tasks = new HashMap<>();
  /** The getter for each tracked condition. */
  private final Map<ConditionId, Condition<? super $Timeline>> conditions = new HashMap<>();
  /** The profiling state for each tracked resource. */
  private final Map<ResourceId, ProfilingState<? super $Timeline, ?>> resources = new HashMap<>();

  /** The task that spawned a given task (if any)). */
  private final Map<TaskId, TaskId> taskParent = new HashMap<>();
  /** The set of children for each task (if any). */
  @DerivedFrom("taskParent")
  private final Map<TaskId, Set<TaskId>> taskChildren = new HashMap<>();
  /** The instantiated input provided to the task. Missing entries indicate tasks without input. */
  private final Map<TaskId, Directive<?, ?>> taskDirective = new HashMap<>();

  /** Construct a task defined by the behavior of a model given a type and arguments. */
  public <Model>
  TaskId initiateTaskFromInput(final Adaptation<? super $Timeline, Model> model, final SerializedActivity input) {
    final var task = TaskId.generate();

    final Directive<Model, ?> directive;
    try {
      directive = model.instantiateDirective(input);
    } catch (final TaskSpecType.UnconstructableTaskSpecException ex) {
      // TODO: Provide more information about the failure.
      this.tasks.put(task, new ExecutionState.IllegalSource<>());

      return task;
    }

    this.tasks.put(task, new ExecutionState.NotStarted<>(() -> directive.createTask(model.getModel())));
    this.taskDirective.put(task, directive);

    return task;
  }

  /** Define a task given a factory method from which that task can be constructed. */
  public TaskId initiateTaskFromSource(final TaskSource<$Timeline> source) {
    final var task = TaskId.generate();
    this.tasks.put(task, new ExecutionState.NotStarted<>(source));
    return task;
  }

  /** Define a task given a black-box task state. */
  public TaskId initiateTask(final Duration startTime, final Task<$Timeline> state) {
    final var task = TaskId.generate();
    this.tasks.put(task, new ExecutionState.InProgress<>(startTime, state));
    return task;
  }

  /** Register a resource whose profile should be accumulated over time. */
  public <ResourceType>
  void trackResource(
      final String name,
      final ResourceSolver<? super $Timeline, ResourceType, ?> solver,
      final ResourceType getter,
      final Duration nextQueryTime
  ) {
    final var resource = new ResourceId(name);

    this.resources.put(resource, ProfilingState.create(getter, solver));
    this.scheduledJobs.schedule(JobId.forResource(resource), SubInstant.Resources.at(nextQueryTime));
  }

  /** Schedule a task to be performed at the given time. Overrides any existing scheduling for the given task. */
  public void scheduleTask(final TaskId task, final Duration scheduleTime) {
    this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(scheduleTime));
  }

  /** Schedules any conditions or resources dependent on the given topic to be re-checked at the given time. */
  public void invalidateTopic(final TopicId topic, final Duration invalidationTime) {
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
    return this.scheduledJobs.extractNextJobs(maximumTime);
  }

  /** Performs a collection of tasks concurrently, extending the given timeline by their stateful effects. */
  public History<$Timeline> performJobs(
      final Collection<JobId> jobs,
      final History<$Timeline> now,
      final Duration currentTime,
      final Duration maximumTime,
      final Adaptation<? super $Timeline, ?> model
  ) {
    return TaskFrame.runToCompletion(jobs, now, (job, builder) -> {
      this.performJob(job, builder, currentTime, maximumTime, model);
    });
  }

  /** Performs a single job. */
  private void performJob(
      final JobId job,
      final TaskFrame.FrameBuilder<$Timeline, JobId> builder,
      final Duration currentTime,
      final Duration maximumTime,
      final Adaptation<? super $Timeline, ?> model
  ) {
    if (job instanceof JobId.TaskJobId j) {
      this.stepTask(j.id(), builder, currentTime, model);
    } else if (job instanceof JobId.SignalJobId j) {
      this.stepSignalledTasks(j.id(), builder);
    } else if (job instanceof JobId.ConditionJobId j) {
      this.updateCondition(j.id(), builder.now(), currentTime, maximumTime);
    } else if (job instanceof JobId.ResourceJobId j) {
      this.updateResource(j.id(), builder.now(), currentTime);
    } else {
      throw new IllegalArgumentException("Unexpected subtype of %s: %s".formatted(JobId.class, job.getClass()));
    }
  }

  /** Perform the next step of a modeled task. */
  public void stepTask(
      final TaskId task,
      final TaskFrame.FrameBuilder<$Timeline, JobId> builder,
      final Duration currentTime,
      final Adaptation<? super $Timeline, ?> model
  ) {
    // The handler for each individual task stage is responsible
    //   for putting an updated lifecycle back into the task set.
    var lifecycle = this.tasks.remove(task);

    // Extract the current modeling state.
    if (lifecycle instanceof ExecutionState.IllegalSource) {
      // pass -- uninstantiable tasks never progress or complete
    } else if (lifecycle instanceof ExecutionState.NotStarted<$Timeline> e) {
      stepEffectModel(task, e.startedAt(currentTime), builder, currentTime, model);
    } else if (lifecycle instanceof ExecutionState.InProgress<$Timeline> e) {
      stepEffectModel(task, e, builder, currentTime, model);
    } else if (lifecycle instanceof ExecutionState.AwaitingChildren<$Timeline> e) {
      stepWaitingTask(task, e, builder, currentTime);
    } else {
      // TODO: Log this issue to somewhere more general than stderr.
      System.err.println("Task %s is ready but in unexpected execution state %s".formatted(task, lifecycle));
    }
  }

  /** Make progress in a task by stepping its associated effect model forward. */
  private void stepEffectModel(
      final TaskId task,
      final ExecutionState.InProgress<$Timeline> progress,
      final TaskFrame.FrameBuilder<$Timeline, JobId> builder,
      final Duration currentTime,
      final Adaptation<? super $Timeline, ?> model
  ) {
    // Step the modeling state forward.
    final var scheduler = new EngineScheduler(model, currentTime, task, builder);
    final var state = progress.state();
    final var status = state.step(scheduler);

    // TODO: Report which topics this activity wrote to at this point in time. This is useful insight for any user.
    // TODO: Report which cells this activity read from at this point in time. This is useful insight for any user.

    // Based on the task's return status, update its execution state and schedule its resumption.
    if (status instanceof TaskStatus.Completed) {
      final var children = new LinkedList<>(this.taskChildren.getOrDefault(task, Collections.emptySet()));

      final var awaiting = progress.completedAt(currentTime, children);
      this.stepWaitingTask(task, awaiting, builder, currentTime);
    } else if (status instanceof TaskStatus.Delayed<?> s) {
      this.tasks.put(task, progress.continueWith(state));
      this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime.plus(s.delay())));
    } else if (status instanceof TaskStatus.AwaitingTask<?> s) {
      this.tasks.put(task, progress.continueWith(state));

      final var target = new TaskId(s.target());
      final var targetExecution = this.tasks.get(target);
      if (targetExecution == null) {
        // TODO: Log that we saw a task ID that doesn't exist. Try to make this as visible as possible to users.
        // pass -- nonexistent tasks will never complete
      } else if (targetExecution instanceof ExecutionState.Terminated) {
        this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(currentTime));
      } else {
        this.waitingTasks.subscribeQuery(task, Set.of(SignalId.forTask(new TaskId(s.target()))));
      }
    } else if (status instanceof TaskStatus.AwaitingCondition<$Timeline> s) {
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
      final ExecutionState.AwaitingChildren<$Timeline> awaiting,
      final TaskFrame.FrameBuilder<$Timeline, JobId> builder,
      final Duration currentTime
  ) {
    // TERMINATION: We break when there are no remaining children,
    //   and we always remove one if we don't break for other reasons.
    while (true) {
      if (awaiting.remainingChildren().isEmpty()) {
        this.tasks.put(task, awaiting.joinedAt(currentTime));
        builder.signal(JobId.forSignal(SignalId.forTask(task)));
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
  public void stepSignalledTasks(final SignalId signal, final TaskFrame.FrameBuilder<$Timeline, JobId> builder) {
    final var tasks = this.waitingTasks.invalidateTopic(signal);
    for (final var task : tasks) builder.signal(JobId.forTask(task));

    if (signal instanceof SignalId.ConditionSignalId s) {
      // Since we're resuming the tasks blocked on this condition,
      // we can (and should!) untrack the condition.
      this.conditions.remove(s.id());
      this.waitingConditions.unsubscribeQuery(s.id());
    }
  }

  /** Determine when a condition is next true, and schedule a signal to be raised at that time. */
  public void updateCondition(final ConditionId condition, final History<$Timeline> now, final Duration currentTime, final Duration horizonTime) {
    final var querier = new EngineQuerier(now);
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
  public void updateResource(final ResourceId resource, final History<$Timeline> now, final Duration currentTime) {
    final var querier = new EngineQuerier(now);
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
      if (task instanceof ExecutionState.InProgress<?> r) {
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
      final SimulationEngine<?> engine,
      final Instant startTime,
      final Duration elapsedTime,
      final Map<String, String> taskToPlannedDirective
  ) {
    final var realProfiles = new HashMap<String, List<Pair<Duration, RealDynamics>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>();

    engine.resources.forEach(new BiConsumer<ResourceId, ProfilingState<?, ?>>() {
      @Override
      public void accept(final ResourceId resource, final ProfilingState<?, ?> state) {
        acceptHelper(resource.id(), state);
      }

      <Dynamics> void acceptHelper(final String name, final ProfilingState<?, Dynamics> state) {
        final var solver = state.getter().solver();
        solver.approximate(new ResourceSolver.ApproximatorVisitor<Dynamics, Void>() {
          @Override
          public Void real(final RealApproximator<Dynamics> approximator) {
            realProfiles.put(name, approximateProfile(approximator));

            return null;
          }

          @Override
          public Void discrete(final DiscreteApproximator<Dynamics> approximator) {
            discreteProfiles.put(name, Pair.of(approximator.getSchema(), approximateProfile(approximator)));

            return null;
          }

          private <Derived>
          List<Pair<Duration, Derived>>
          approximateProfile(final Approximator<Dynamics, Derived> approximator) {
            final var profile = new ArrayList<Pair<Duration, Derived>>();

            final var segmentsIter = state.profile().iterator();
            if (segmentsIter.hasNext()) {
              var segment = segmentsIter.next();
              while (segmentsIter.hasNext()) {
                final var nextSegment = segmentsIter.next();

                final var segmentEnd = nextSegment.startOffset();
                final var segmentStart = segment.startOffset();
                final var segmentDuration = segmentEnd.minus(segmentStart);

                final var approximation = approximator.approximate(segment.dynamics()).iterator();
                var partStart = segmentStart;
                do {
                  final var part = approximation.next();
                  final var partExtent = Duration.min(part.extent, segmentDuration);

                  profile.add(Pair.of(partExtent, part.dynamics));
                  partStart = partStart.plus(partExtent);
                } while (approximation.hasNext() && partStart.shorterThan(segmentEnd));

                segment = nextSegment;
              }

              {
                final var segmentStart = segment.startOffset();
                final var segmentEnd = elapsedTime;
                final var segmentDuration = segmentEnd.minus(segmentStart);

                final var approximation = approximator.approximate(segment.dynamics()).iterator();
                var partStart = segmentStart;
                do {
                  final var part = approximation.next();
                  final var partExtent = Duration.min(part.extent, segmentDuration);

                  profile.add(Pair.of(partExtent, part.dynamics));
                  partStart = partStart.plus(partExtent);
                } while (approximation.hasNext() && partStart.noLongerThan(segmentEnd));
              }
            }

            return profile;
          }
        });
      }
    });

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
            activityChildren.getOrDefault(activityId, Collections.emptyList())
        ));
      } else {
        unsimulatedActivities.put(activityId, new SerializedActivity(
            directive.getType(),
            directive.getArguments()));
      }
    });

    return new SimulationResults(realProfiles, discreteProfiles, simulatedActivities, unsimulatedActivities, startTime);
  }

  /** A handle for processing requests from a modeled resource or condition. */
  private final class EngineQuerier implements Querier<$Timeline> {
    private final History<$Timeline> history;
    private final Set<TopicId> referencedTopics = new HashSet<>();
    private Optional<Duration> expiry = Optional.empty();

    public EngineQuerier(final History<$Timeline> history) {
      this.history = Objects.requireNonNull(history);
    }

    @Override
    public <State> State getState(final Query<? super $Timeline, ?, State> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via AdaptationBuilder).
      @SuppressWarnings("unchecked")
      final var query = ((EngineQuery<? super $Timeline, ?, State>) token).query();

      // TODO: Cache the state (until the query returns) to avoid unnecessary copies
      //  if the same state is requested multiple times in a row.
      final var state = this.history.ask(query);

      this.expiry = map2(this.expiry, query.getCurrentExpiry(state), Duration::min);
      this.referencedTopics.add(new TopicId(query.getTableIndex()));

      return state;
    }

    private static <T> Optional<T> map2(final Optional<T> a, final Optional<T> b, final BinaryOperator<T> f) {
      if (a.isEmpty()) return b;
      if (b.isEmpty()) return a;
      return Optional.of(f.apply(a.get(), b.get()));
    }
  }

  /** A handle for processing requests and effects from a modeled task. */
  private final class EngineScheduler implements Scheduler<$Timeline> {
    private final Adaptation<? super $Timeline, ?> model;

    private final Duration currentTime;
    private final TaskId activeTask;
    private final TaskFrame.FrameBuilder<$Timeline, JobId> builder;
    private final Set<TopicId> referencedTopics = new HashSet<>();
    private final Set<TopicId> affectedTopics = new HashSet<>();

    public EngineScheduler(
        final Adaptation<? super $Timeline, ?> model,
        final Duration currentTime,
        final TaskId activeTask,
        final TaskFrame.FrameBuilder<$Timeline, JobId> builder
    ) {
      this.model = Objects.requireNonNull(model);
      this.currentTime = Objects.requireNonNull(currentTime);
      this.activeTask = Objects.requireNonNull(activeTask);
      this.builder = Objects.requireNonNull(builder);
    }

    @Override
    public <State> State get(final Query<? super $Timeline, ?, State> token) {
      // SAFETY: The only queries the model should have are those provided by us (e.g. via AdaptationBuilder).
      @SuppressWarnings("unchecked")
      final var query = ((EngineQuery<? super $Timeline, ?, State>) token).query();

      this.referencedTopics.add(new TopicId(query.getTableIndex()));

      // TODO: Cache the return value (until the next emit or until the task yields) to avoid unnecessary copies
      //  if the same state is requested multiple times in a row.
      return this.builder.now().ask(query);
    }

    @Override
    public <Event> void emit(final Event event, final Query<? super $Timeline, ? super Event, ?> token) {
      final TopicId topic;

      // Append this event to the timeline.
      {
        // SAFETY: The only queries the model should have are those provided by us (e.g. via AdaptationBuilder).
        @SuppressWarnings("unchecked")
        final var query = ((EngineQuery<? super $Timeline, ? super Event, ?>) token).query();

        this.builder.emit(event, query);

        topic = new TopicId(query.getTableIndex());
      }

      this.affectedTopics.add(topic);
      SimulationEngine.this.invalidateTopic(topic, this.currentTime);
    }

    @Override
    public String spawn(final Task<$Timeline> state) {
      final var task = TaskId.generate();
      SimulationEngine.this.tasks.put(task, new ExecutionState.InProgress<>(this.currentTime, state));
      SimulationEngine.this.taskParent.put(task, this.activeTask);
      SimulationEngine.this.taskChildren.computeIfAbsent(this.activeTask, $ -> new HashSet<>()).add(task);
      this.builder.signal(JobId.forTask(task));

      return task.id();
    }

    @Override
    public String spawn(final String type, final Map<String, SerializedValue> arguments) {
      final var task = initiateTaskFromInput(this.model, new SerializedActivity(type, arguments));
      SimulationEngine.this.taskParent.put(task, this.activeTask);
      SimulationEngine.this.taskChildren.computeIfAbsent(this.activeTask, $ -> new HashSet<>()).add(task);
      this.builder.signal(JobId.forTask(task));

      return task.id();
    }

    @Override
    public String defer(final Duration delay, final Task<$Timeline> state) {
      if (delay.isNegative()) throw new IllegalArgumentException("Cannot schedule a task in the past");

      final var task = TaskId.generate();
      SimulationEngine.this.tasks.put(task, new ExecutionState.InProgress<>(this.currentTime, state));
      SimulationEngine.this.taskParent.put(task, this.activeTask);
      SimulationEngine.this.taskChildren.computeIfAbsent(this.activeTask, $ -> new HashSet<>()).add(task);
      SimulationEngine.this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(this.currentTime.plus(delay)));

      return task.id();
    }

    @Override
    public String defer(final Duration delay, final String type, final Map<String, SerializedValue> arguments) {
      if (delay.isNegative()) throw new IllegalArgumentException("Cannot schedule a task in the past");

      final var task = initiateTaskFromInput(this.model, new SerializedActivity(type, arguments));
      SimulationEngine.this.taskParent.put(task, this.activeTask);
      SimulationEngine.this.taskChildren.computeIfAbsent(this.activeTask, $ -> new HashSet<>()).add(task);
      SimulationEngine.this.scheduledJobs.schedule(JobId.forTask(task), SubInstant.Tasks.at(this.currentTime.plus(delay)));

      return task.id();
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
  private sealed interface ExecutionState<$Timeline> {
    /** The task has an invalid source for its behavior. */
    // TODO: Provide more details about the instantiation failure.
    record IllegalSource<$Timeline>()
        implements ExecutionState<$Timeline> {}

    /** The task has not yet started. */
    record NotStarted<$Timeline>(TaskSource<$Timeline> source)
        implements ExecutionState<$Timeline>
    {
      public InProgress<$Timeline> startedAt(final Duration startOffset) {
        return new InProgress<>(startOffset, this.source.createTask());
      }
    }

    /** The task is in its primary operational phase. */
    record InProgress<$Timeline>(Duration startOffset, Task<$Timeline> state)
        implements ExecutionState<$Timeline>
    {
      public AwaitingChildren<$Timeline> completedAt(final Duration endOffset, final LinkedList<TaskId> remainingChildren) {
        return new AwaitingChildren<>(this.startOffset, endOffset, remainingChildren);
      }

      public InProgress<$Timeline> continueWith(final Task<$Timeline> newState) {
        return new InProgress<>(this.startOffset, newState);
      }
    }

    /** The task has completed its primary operation, but has unfinished children. */
    record AwaitingChildren<$Timeline>(Duration startOffset, Duration endOffset, LinkedList<TaskId> remainingChildren)
        implements ExecutionState<$Timeline>
    {
      public Terminated<$Timeline> joinedAt(final Duration joinOffset) {
        return new Terminated<>(this.startOffset, this.endOffset, joinOffset);
      }
    }

    /** The task and all its delegated children have completed. */
    record Terminated<$Timeline>(Duration startOffset, Duration endOffset, Duration joinOffset)
        implements ExecutionState<$Timeline> {}
  }
}
