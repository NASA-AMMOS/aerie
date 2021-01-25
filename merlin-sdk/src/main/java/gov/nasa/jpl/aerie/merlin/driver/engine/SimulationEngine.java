package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * An engine for driving scheduled simulation tasks over time.
 */
public final class SimulationEngine<$Timeline> {
  // A time-ordered queue of all scheduled tasks.
  private final PriorityQueue<Triple<Duration, String, Task<$Timeline>>> queue =
      new PriorityQueue<>(Comparator.comparing(Triple::getLeft));
  // The set of all tasks that have completed.
  private final Set<String> completed = new HashSet<>();

  // The elapsed time when a task was first resumed.
  private final Map<String, Duration> taskStartTimes = new HashMap<>();
  // The elapsed time when a task reported completion.
  private final Map<String, Duration> taskEndTimes = new HashMap<>();
  // For each task, a set of tasks awaiting its completion.
  private final Map<String, Set<Pair<String, Task<$Timeline>>>> conditioned = new HashMap<>();
  // For each task, a record of its type, arguments and parent task id.
  private final Map<String, TaskRecord> taskRecords = new HashMap<>();

  // The history of events produced by tasks.
  private History<$Timeline> currentHistory;
  // The elapsed simulation time since creating this engine.
  private Duration elapsedTime = Duration.ZERO;
  // The next available task id.
  private Integer nextTaskId = 0;
  private final BiFunction<String, Map<String, SerializedValue>, Task<$Timeline>> createTask;

  public SimulationEngine(
      final History<$Timeline> initialHistory,
      final BiFunction<String, Map<String, SerializedValue>, Task<$Timeline>> createTask) {
    this.currentHistory = initialHistory;
    this.createTask = createTask;
  }

  private String generateId() {
    final var id = this.nextTaskId.toString();
    this.nextTaskId++;
    return id;
  }

  private void enqueue(final String id, final Duration delay, final Task<$Timeline> task) {
    this.queue.add(Triple.of(this.elapsedTime.plus(delay), id, task));
  }

  /**
   * Schedule a task to be run after a given duration of simulated time.
   *
   * @param delay The amount of time to wait before performing the task
   * @param spec The task specification
   * @param type The type of task specification
   * @return The unique identifier assigned to the created task
   */
  public <Spec> String defer(Duration delay, Spec spec, TaskSpecType<? super $Timeline, Spec> type) {
    final var id = this.generateId();
    final var record = new TaskRecord(type.getName(), type.getArguments(spec), Optional.empty());

    this.taskRecords.put(id, record);
    this.enqueue(id, delay, type.createTask(spec));
    return id;
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
      if (this.queue.isEmpty() || endTime.shorterThan(this.queue.peek().getLeft())) {
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
    final var nextJobTime = this.queue.peek().getLeft();
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
    final var nextJobTime = this.queue.peek().getLeft();

    var tip = this.currentHistory;
    var spawns = new ArrayDeque<Triple<History<$Timeline>, String, Task<$Timeline>>>();

    while (!this.queue.isEmpty() && this.queue.peek().getLeft().isEqualTo(nextJobTime)) {
      tip = tip.fork();
      final var entry = this.queue.poll();
      spawns.push(Triple.of(tip, entry.getMiddle(), entry.getRight()));
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
        final var branchTip = branch.getLeft();
        final var taskId = branch.getMiddle();
        final var task = branch.getRight();

        final var scheduler = new EngineScheduler(branchTip, taskId);
        final var status = task.step(scheduler);

        this.taskStartTimes.putIfAbsent(taskId, this.elapsedTime);
        status.match(new StatusVisitor(task, taskId));
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

  public Map<String, TaskRecord> getTaskRecords() {
    return Collections.unmodifiableMap(this.taskRecords);
  }

  public Map<String, Window> getTaskWindows() {
    final var windows = new HashMap<String, Window>();

    for (final var endEntry : this.taskEndTimes.entrySet()) {
      final var taskId = endEntry.getKey();
      final var endTime = endEntry.getValue();

      final var startTime = this.taskStartTimes.get(taskId);

      // TODO: handle the case in which a task never ends.
      //  This could be to provide an end time based on the initial plan
      //  end time
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
      builder.append(String.format("%10s: %s\n", point.getLeft(), point.getMiddle()));
    }

    return builder.toString();
  }

  private final class EngineScheduler implements Scheduler<$Timeline> {
    public History<$Timeline> now;
    public final Deque<Triple<History<$Timeline>, String, Task<$Timeline>>> branches = new ArrayDeque<>();
    private final String parentTaskId;

    public EngineScheduler(final History<$Timeline> now, final String parentTaskId) {
      this.now = now;
      this.parentTaskId = parentTaskId;
    }

    @Override
    public History<$Timeline> now() {
      return this.now;
    }

    @Override
    public <Event> void emit(final Event event, final Query<? super $Timeline, Event, ?> query) {
      this.now = this.now.emit(event, query);
    }

    @Override
    public String spawn(final String type, final Map<String, SerializedValue> arguments) {
      final var id = SimulationEngine.this.generateId();
      final var record = new TaskRecord(type, arguments, Optional.of(this.parentTaskId));
      final var task = SimulationEngine.this.createTask.apply(type, arguments);

      this.now = this.now.fork();
      this.branches.push(Triple.of(this.now, id, task));

      SimulationEngine.this.taskRecords.put(id, record);
      return id;
    }

    @Override
    public String defer(final Duration delay, final String type, final Map<String, SerializedValue> arguments) {
      final var id = SimulationEngine.this.generateId();
      final var record = new TaskRecord(type, arguments, Optional.of(this.parentTaskId));
      final var task = SimulationEngine.this.createTask.apply(type, arguments);

      SimulationEngine.this.enqueue(id, delay, task);

      SimulationEngine.this.taskRecords.put(id, record);
      return id;
    }

  }

  private final class StatusVisitor implements TaskStatus.Visitor<$Timeline, Object> {
    private final Task<$Timeline> task;
    private final String taskId;

    public StatusVisitor(final Task<$Timeline> task, final String taskId) {
      this.task = task;
      this.taskId = taskId;
    }

    @Override
    public Object completed() {
      SimulationEngine.this.completed.add(this.taskId);
      SimulationEngine.this.taskEndTimes.put(this.taskId, SimulationEngine.this.elapsedTime);

      final var conditionedActivities = SimulationEngine.this.conditioned.remove(this.taskId);
      if (conditionedActivities != null) {
        for (final var conditionedTask : conditionedActivities) {
          SimulationEngine.this.enqueue(conditionedTask.getLeft(), Duration.ZERO, conditionedTask.getRight());
        }
      }
      return null;
    }

    @Override
    public Object delayed(final Duration delay) {
      SimulationEngine.this.enqueue(this.taskId, delay, this.task);
      return null;
    }

    @Override
    public Object awaiting(final String activityId) {
      if (SimulationEngine.this.completed.contains(activityId)) {
        SimulationEngine.this.enqueue(taskId, Duration.ZERO, task);
      } else {
        SimulationEngine.this.conditioned.computeIfAbsent(activityId, k -> new HashSet<>()).add(Pair.of(taskId, task));
      }
      return null;
    }

    @Override
    public Object awaiting(final Condition<? super $Timeline> condition) {
      // TODO: work out how to await a task on conditions
      // this is a hack which will be removed when awaiting on a condition
      // has an implementation path.
      SimulationEngine.this.enqueue(this.taskId, Duration.ZERO, this.task);
      return null;
    }
  }
}
