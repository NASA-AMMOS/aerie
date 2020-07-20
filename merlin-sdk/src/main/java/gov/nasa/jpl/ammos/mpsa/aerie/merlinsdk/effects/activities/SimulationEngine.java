package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

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

  public void enqueue(final Duration timeFromStart, final SimulationTask<T, Event> activity) {
    this.schedule(new ScheduleItem.Defer<>(timeFromStart.minus(this.elapsedTime), activity));
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

    var tip = this.currentHistory.wait(nextJobTime.minus(this.elapsedTime));

    // Extract all events occurring at this time.
    // More events might be added at this same time, so to ensure coherency, we handle events in complete batches.
    final var eventList = new ArrayDeque<Pair<History<T, Event>, SimulationTask<T, Event>>>();
    while (!this.queue.isEmpty() && this.queue.peek().getKey().equals(nextJobTime)) {
      tip = tip.fork();
      eventList.push(Pair.of(tip, this.queue.poll().getValue()));
    }

    // React to each event.
    while (!eventList.isEmpty()) {
      final var eventPair = eventList.pop();
      final var eventTime = eventPair.getKey();
      final var task = eventPair.getValue();

      final var result = task.runFrom(eventTime);
      final var endTime = result.getLeft();
      final var scheduledTasks = result.getRight();

      tip = tip.join(endTime);
      scheduledTasks.values().forEach(this::schedule);
    }

    this.currentHistory = tip;
    this.elapsedTime = nextJobTime;
  }

  private void schedule(final ScheduleItem<T, Event> rule) {
    if (this.completed.contains(rule.getTaskId())) {
      throw new RuntimeException("Illegal attempt to re-process a completed task: " + rule);
    }

    // This just screams for case classes and pattern-matching `switch`.
    if (rule instanceof ScheduleItem.Defer) {
      final var duration = ((ScheduleItem.Defer<T, Event>) rule).duration;
      final var activity = ((ScheduleItem.Defer<T, Event>) rule).activity;

      this.queue.add(Pair.of(this.elapsedTime.plus(duration), activity));
    } else if (rule instanceof ScheduleItem.OnCompletion) {
      final var waitId = ((ScheduleItem.OnCompletion<T, Event>) rule).waitOn;
      final var activity = ((ScheduleItem.OnCompletion<T, Event>) rule).activity;

      if (this.completed.contains(waitId)) {
        this.queue.add(Pair.of(this.elapsedTime, activity));
      } else {
        this.conditioned.computeIfAbsent(waitId, k -> new HashSet<>()).add(activity);
      }
    } else if (rule instanceof ScheduleItem.Complete) {
      final var activityId = ((ScheduleItem.Complete<T, Event>) rule).activityId;
      this.completed.add(activityId);

      final var conditionedActivities = this.conditioned.remove(activityId);

      if (conditionedActivities != null) {
        for (final var conditionedTask : conditionedActivities) {
          this.queue.add(Pair.of(this.elapsedTime, conditionedTask));
        }
      }
    } else {
      throw new Error(String.format(
          "Unknown subclass `%s` of class `%s`",
          rule.getClass().getName(),
          ScheduleItem.class.getName()));
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
