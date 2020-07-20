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

public final class SimulationEngine<T, Event> {
  private final PriorityQueue<Pair<Duration, SimulationTask<T, Event>>> queue =
      new PriorityQueue<>(Comparator.comparing(Pair::getKey));
  private final Set<String> completed = new HashSet<>();
  private final Map<String, Set<SimulationTask<T, Event>>> conditioned = new HashMap<>();

  private History<T, Event> currentHistory;
  private Duration elapsedTime = Duration.ZERO;

  public SimulationEngine(final History<T, Event> initialHistory) {
    this.currentHistory = initialHistory;
  }

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

  public void runFor(final long quantity, final Duration unit) {
    this.runFor(Duration.of(quantity, unit));
  }

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

  public void step() {
    if (this.queue.isEmpty()) return;

    // Step up to the next job time.
    final var nextJobTime = this.queue.peek().getKey();
    this.currentHistory = this.currentHistory.wait(nextJobTime.minus(this.elapsedTime));
    this.elapsedTime = nextJobTime;

    // Process each task at this time, and any spawned sub-tasks.
    final var frames = new ArrayDeque<TaskFrame<T, Event>>();
    frames.push(this.getNextRootFrame());
    this.currentHistory = this.processFrameStack(frames);
  }

  private TaskFrame<T, Event> getNextRootFrame() {
    assert !this.queue.isEmpty();
    final var nextJobTime = this.queue.peek().getKey();

    // Extract all events occurring at this time.
    var tip = this.currentHistory;
    var spawns = new ArrayDeque<Pair<History<T, Event>, SimulationTask<T, Event>>>();
    while (!this.queue.isEmpty() && this.queue.peek().getKey().equals(nextJobTime)) {
      tip = tip.fork();
      spawns.push(Pair.of(tip, this.queue.poll().getValue()));
    }

    return new TaskFrame<>(tip, spawns);
  }

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
        // Pop this completed sub-task.
        final var frame = stack.pop();

        if (stack.isEmpty()) return frame.tip;

        final var parent = stack.peek();
        parent.tip = parent.tip.join(frame.tip);
      }
    }
  }

  public boolean hasMoreJobs() {
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
