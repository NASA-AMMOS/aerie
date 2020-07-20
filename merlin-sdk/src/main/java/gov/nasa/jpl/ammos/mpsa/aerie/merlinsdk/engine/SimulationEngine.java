package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * An engine for driving scheduled simulation tasks over time.
 */
public final class SimulationEngine<T, Event> {
  // A time-ordered queue of all scheduled tasks.
  private final PriorityQueue<Pair<Duration, SimulationTask<T, Event>>> queue =
      new PriorityQueue<>(Comparator.comparing(Pair::getKey));
  // The set of all tasks that have completed.
  private final Set<String> completed = new HashSet<>();
  // For each task, a set of tasks awaiting its completion.
  private final Map<String, Set<SimulationTask<T, Event>>> conditioned = new HashMap<>();


  // The history of events produced by tasks.
  private History<T, Event> currentHistory;
  // The elapsed simulation time since creating this engine.
  private Duration elapsedTime = Duration.ZERO;

  public SimulationEngine(final History<T, Event> initialHistory) {
    this.currentHistory = initialHistory;
  }

  /**
   * Schedule a task to be run after a given duration of simulated time.
   *
   * @param delay The amount of time to wait before performing the task
   * @param task The task to perform.
   */
  public void defer(final Duration delay, final SimulationTask<T, Event> task) {
    this.queue.add(Pair.of(this.elapsedTime.plus(delay), task));
  }

  public void await(final String taskToAwait, final SimulationTask<T, Event> task) {
    if (this.completed.contains(taskToAwait)) {
      this.queue.add(Pair.of(this.elapsedTime, task));
    } else {
      this.conditioned.computeIfAbsent(taskToAwait, k -> new HashSet<>()).add(task);
    }
  }

  /*package-local*/
  void markCompleted(final String taskId) {
    this.completed.add(taskId);

    final var conditionedActivities = this.conditioned.remove(taskId);
    if (conditionedActivities == null) return;

    for (final var conditionedTask : conditionedActivities) {
      this.queue.add(Pair.of(this.elapsedTime, conditionedTask));
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
    final var frames = new ArrayDeque<TaskFrame<T, Event>>();
    frames.push(this.extractNextRootFrame());
    this.currentHistory = this.processFrameStack(frames);
  }

  // A "root frame" is the set of all tasks scheduled for a given time.
  // The "next" root frame is the earliest of these.
  private TaskFrame<T, Event> extractNextRootFrame() {
    assert !this.queue.isEmpty();
    final var nextJobTime = this.queue.peek().getKey();

    var tip = this.currentHistory;
    var spawns = new ArrayDeque<Pair<History<T, Event>, SimulationTask<T, Event>>>();
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
  private History<T, Event> processFrameStack(final ArrayDeque<TaskFrame<T, Event>> stack) {
    while (true) {
      assert !stack.isEmpty();

      if (!stack.peek().branches.isEmpty()) {
        // Execute the next sub-task.
        final var frame = stack.peek();
        final var branch = frame.popBranch();
        final var branchTip = branch.getKey();
        final var task = branch.getValue();

        final var scheduler = new EngineTaskScheduler<>(this);
        final var endTime = task.runFrom(branchTip, scheduler);

        stack.push(new TaskFrame<>(endTime, scheduler.getBranches()));
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

  public History<T, Event> getCurrentHistory() {
    return this.currentHistory;
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
}
