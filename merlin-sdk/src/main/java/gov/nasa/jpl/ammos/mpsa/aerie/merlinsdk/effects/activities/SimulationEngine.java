package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
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

public final class SimulationEngine<T, Event, Activity extends SimulationTask> {
  private final PriorityQueue<Pair<Duration, ResumeActivityEvent<Activity>>> queue =
      new PriorityQueue<>(Comparator.comparing(Pair::getKey));
  private final Set<String> completed = new HashSet<>();
  private final Map<String, Set<ResumeActivityEvent<Activity>>> conditioned = new HashMap<>();

  private final Projection<ResumeActivityEvent<Activity>, ? extends Task<T, Event, Activity>> reactor;

  private History<T, Event> currentHistory;
  private Duration elapsedTime = Duration.ZERO;

  public <TaskType extends Task<T, Event, Activity>> SimulationEngine(
      final History<T, Event> initialHistory,
      final Projection<ResumeActivityEvent<Activity>, TaskType> reactor)
  {
    this.reactor = reactor;
    this.currentHistory = initialHistory;
  }

  public void enqueue(final Duration timeFromStart, final Activity activity) {
    this.schedule(activity.getId(), new ScheduleItem.Defer<>(timeFromStart.minus(this.elapsedTime), activity));
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
    final var eventList = new ArrayDeque<Pair<History<T, Event>, ResumeActivityEvent<Activity>>>();
    while (!this.queue.isEmpty() && this.queue.peek().getKey().equals(nextJobTime)) {
      tip = tip.fork();
      eventList.push(Pair.of(tip, this.queue.poll().getValue()));
    }

    // React to each event.
    while (!eventList.isEmpty()) {
      final var eventPair = eventList.pop();
      final var eventTime = eventPair.getKey();
      final var event = eventPair.getValue();

      final var result = this.reactor.atom(event).apply(eventTime);
      final var endTime = result.getLeft();
      final var scheduledItems = result.getRight();

      tip = tip.join(endTime);
      for (final var entry : scheduledItems.entrySet()) {
        this.schedule(entry.getKey(), entry.getValue());
      }
    }

    this.currentHistory = tip;
    this.elapsedTime = nextJobTime;
  }

  private void schedule(final String activityId, final ScheduleItem<Activity> rule) {
    if (this.completed.contains(activityId)) {
      throw new RuntimeException("Illegal attempt to re-process a completed task: " + rule);
    }

    // This just screams for case classes and pattern-matching `switch`.
    if (rule instanceof ScheduleItem.Defer) {
      final var duration = ((ScheduleItem.Defer<Activity>) rule).duration;
      final var activity = ((ScheduleItem.Defer<Activity>) rule).activity;

      this.queue.add(Pair.of(this.elapsedTime.plus(duration), new ResumeActivityEvent<>(activityId, activity)));
    } else if (rule instanceof ScheduleItem.OnCompletion) {
      final var waitId = ((ScheduleItem.OnCompletion<Activity>) rule).waitOn;
      final var activity = ((ScheduleItem.OnCompletion<Activity>) rule).activity;

      final var resumption = new ResumeActivityEvent<>(activityId, activity);
      if (this.completed.contains(waitId)) {
        this.queue.add(Pair.of(this.elapsedTime, resumption));
      } else {
        this.conditioned.computeIfAbsent(waitId, k -> new HashSet<>()).add(resumption);
      }
    } else if (rule instanceof ScheduleItem.Complete) {
      this.completed.add(((ScheduleItem.Complete<Activity>) rule).activityId);

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
