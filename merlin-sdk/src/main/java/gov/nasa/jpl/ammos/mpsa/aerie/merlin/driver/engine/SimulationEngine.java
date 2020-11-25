package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SolvableDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * An engine for driving scheduled simulation tasks over time.
 */
public final class SimulationEngine<$Timeline> {
  // A time-ordered queue of all scheduled tasks.
  private final PriorityQueue<Pair<Duration, Task<$Timeline>>> queue =
      new PriorityQueue<>(Comparator.comparing(Pair::getKey));
  // The set of all tasks that have completed.
  private final Set<String> completed = new HashSet<>();
  // For each task, a set of tasks awaiting its completion.
  private final Map<String, Set<Task<$Timeline>>> conditioned = new HashMap<>();

  // The elapsed time when a task was first resumed.
  private final Map<String, Duration> taskStartTimes = new HashMap<>();
  // The elapsed time when a task reported completion.
  private final Map<String, Duration> taskEndTimes = new HashMap<>();

  // The history of events produced by tasks.
  private History<$Timeline> currentHistory;
  // The elapsed simulation time since creating this engine.
  private Duration elapsedTime = Duration.ZERO;

  public SimulationEngine(final History<$Timeline> initialHistory) {
    this.currentHistory = initialHistory;
  }

  /**
   * Schedule a task to be run after a given duration of simulated time.
   *
   * @param delay The amount of time to wait before performing the task
   * @param task The task to perform.
   */
  public void defer(final Duration delay, final Task<$Timeline> task) {
    this.queue.add(Pair.of(this.elapsedTime.plus(delay), task));
  }

  public void await(final String taskToAwait, final Task<$Timeline> task) {
    if (this.completed.contains(taskToAwait)) {
      this.defer(Duration.ZERO, task);
    } else {
      this.conditioned.computeIfAbsent(taskToAwait, k -> new HashSet<>()).add(task);
    }
  }

  /*package-local*/
  void markCompleted(final String taskId) {
    this.completed.add(taskId);
    this.taskEndTimes.put(taskId, this.elapsedTime);

    final var conditionedActivities = this.conditioned.remove(taskId);
    if (conditionedActivities == null) return;

    for (final var conditionedTask : conditionedActivities) {
      this.defer(Duration.ZERO, conditionedTask);
    }
  }

  /** @see #runFor(Duration) */
  public void runFor(final long quantity, final Duration unit) {
    this.runFor(Duration.of(quantity, unit));
  }

  /**
   * Run the simulation for the given duration of simulated time.
   *
   * When this method returns, there will be no remaining tasks at the current time.
   *
   * @param duration The amount of simulation time to run the simulation for.
   */
  public void runFor(final Duration duration) {
    final var endTime = this.elapsedTime.plus(duration);
    while (!endTime.shorterThan(this.elapsedTime)) {
      // If there are no jobs remaining, or the next job is after the end time, simply step up to the end time.
      if (this.queue.isEmpty() || endTime.shorterThan(this.queue.peek().getKey())) {
        this.currentHistory = this.currentHistory.wait(endTime.minus(this.elapsedTime));
        this.elapsedTime = endTime;
        break;
      }

      // Step up to, and perform, the next job.
      this.step();
    }
  }

  /**
   * Run the simulation up to (and including) the next set of simultaneous tasks.
   */
  public void step() {
    if (this.queue.isEmpty()) return;

    // Step up to the next job time.
    final var nextJobTime = this.queue.peek().getKey();
    this.currentHistory = this.currentHistory.wait(nextJobTime.minus(this.elapsedTime));
    this.elapsedTime = nextJobTime;

    // Process each task at this time, and any spawned sub-tasks.
    final var frames = new ArrayDeque<TaskFrame<$Timeline>>();
    frames.push(this.extractNextRootFrame());
    this.currentHistory = this.processFrameStack(frames);
  }

  // A "root frame" is the set of all tasks scheduled for a given time.
  // The "next" root frame is the earliest of these.
  private TaskFrame<$Timeline> extractNextRootFrame() {
    assert !this.queue.isEmpty();
    final var nextJobTime = this.queue.peek().getKey();

    var tip = this.currentHistory;
    var spawns = new ArrayDeque<Pair<History<$Timeline>, Task<$Timeline>>>();
    while (!this.queue.isEmpty() && this.queue.peek().getKey().equals(nextJobTime)) {
      tip = tip.fork();
      spawns.push(Pair.of(tip, this.queue.poll().getValue()));
    }

    return new TaskFrame<>(tip, spawns);
  }

  // A TaskFrame is the result of running a task. It contains the end time of the given top-level task,
  // as well as a list of immediate sub-tasks spawned during its execution.
  // The true end time of a task requires running these sub-tasks, then joining their end times together
  // with the top-level task's end time.
  // Since every sub-task may itself spawn additional sub-tasks, we perform a depth-first traversal
  // of the task tree.
  private History<$Timeline> processFrameStack(final ArrayDeque<TaskFrame<$Timeline>> stack) {
    while (true) {
      assert !stack.isEmpty();

      if (!stack.peek().branches.isEmpty()) {
        // Execute the next sub-task.
        final var frame = stack.peek();
        final var branch = frame.popBranch();
        final var branchTip = branch.getKey();
        final var task = branch.getValue();

        final var scheduler = new EngineScheduler(branchTip);
        final var status = task.step(scheduler);

        status.match(new StatusVisitor(task));
        stack.push(new TaskFrame<>(scheduler.now, scheduler.branches));
      } else {
        // Join this completed sub-task with its parent.
        final var frame = stack.pop();

        if (stack.isEmpty()) return frame.tip;

        final var parent = stack.peek();
        parent.tip = parent.tip.join(frame.tip);
      }
    }
  }

  public boolean hasMoreTasks() {
    return !this.queue.isEmpty();
  }

  public History<$Timeline> getCurrentHistory() {
    return this.currentHistory;
  }

  public Map<String, Window> getTaskWindows() {
    final var windows = new HashMap<String, Window>();

    for (final var endEntry : this.taskEndTimes.entrySet()) {
      final var taskId = endEntry.getKey();
      final var endTime = endEntry.getValue();

      final var startTime = this.taskStartTimes.get(taskId);

      windows.put(taskId, Window.between(startTime, endTime));
    }

    return windows;
  }

  public Duration getElapsedTime() {
    return this.elapsedTime;
  }

  public String getDebugTrace() {
    final var builder = new StringBuilder();

    builder.append(this.currentHistory.getDebugTrace());
    for (final var point : this.queue) {
      builder.append(String.format("%10s: %s\n", point.getKey(), point.getValue()));
    }

    return builder.toString();
  }

  private final class EngineScheduler implements Scheduler<$Timeline> {
    public History<$Timeline> now;
    public final Deque<Pair<History<$Timeline>, Task<$Timeline>>> branches = new ArrayDeque<>();

    public EngineScheduler(final History<$Timeline> now) {
      this.now = now;
    }

    @Override
    public History<$Timeline> now() {
      return this.now;
    }

    @Override
    public <Solution> Solution ask(final SolvableDynamics<Solution, ?> resource, final Duration offset) {
      return resource.solve(new SolvableDynamics.Visitor() {
        @Override
        public Double real(final RealDynamics dynamics) {
          return new RealSolver().valueAt(dynamics, offset);
        }

        @Override
        public <ResourceType> ResourceType discrete(final ResourceType fact) {
          return fact;
        }
      });
    }

    @Override
    public <Event> void emit(final Event event, final Query<? super $Timeline, Event, ?> query) {
      this.now = this.now.emit(event, query);
    }

    @Override
    public <Spec> String spawn(final Spec spec, final TaskSpecType<? super $Timeline, Spec> type) {
      this.now = this.now.fork();
      this.branches.push(Pair.of(this.now, type.createTask(spec)));
      // TODO: return a real task ID. This should be accounted for with [AERIE-1010]
      return "";
    }

    @Override
    public <Spec> String defer(final Duration delay, final Spec spec, final TaskSpecType<? super $Timeline, Spec> type) {
      SimulationEngine.this.defer(delay, type.createTask(spec));
      // TODO: return a real task ID. This should be accounted for with [AERIE-1010]
      return "";
    }
  }

  private final class StatusVisitor implements TaskStatus.Visitor<$Timeline, Object> {
    private final Task<$Timeline> task;

    public StatusVisitor(final Task<$Timeline> task) {this.task = task;}

    @Override
    public Object completed() {
      // TODO: add task id to completed list
      return null;
    }

    @Override
    public Object awaiting(final String activityId) {
      // TODO: figure out how to wait on other tasks
      return null;
    }

    @Override
    public Object delayed(final Duration delay) {
      SimulationEngine.this.defer(delay, this.task);
      return null;
    }

    @Override
    public <ResourceType, ConditionType> Object awaiting(
        final Resource<History<$Timeline>, SolvableDynamics<ResourceType, ConditionType>> resource,
        final ConditionType condition)
    {
      // TODO: work out how to await a task on conditions
      return null;
    }
  }
}
