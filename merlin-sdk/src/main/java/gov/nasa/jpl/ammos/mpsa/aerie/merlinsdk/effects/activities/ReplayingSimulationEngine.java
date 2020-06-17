package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections.EventGraphProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.atom;

public final class ReplayingSimulationEngine<T, Activity, Event> {
  private final PriorityQueue<Pair<Duration, EventGraph<SchedulingEvent<T, Activity, Event>>>> queue = new PriorityQueue<>(Comparator.comparing(Pair::getKey));
  private final Set<String> completed = new HashSet<>();
  private final Map<String, Set<Triple<String, Activity, PVector<ActivityBreadcrumb<T, Event>>>>> conditioned = new HashMap<>();

  private final ActivityReactor<T, Activity, Event> reactor;

  private Time<T, Event> now;
  private Duration currentTime = Duration.ZERO;

  public ReplayingSimulationEngine(
      final Time<T, Event> startTime,
      final BiConsumer<ReactionContext<T, Activity, Event>, Activity> executor
  ) {
    this.reactor = new ActivityReactor<>(executor);
    this.now = startTime;
  }

  public void enqueue(final Duration timeFromStart, final Activity activity) {
    this.queue.add(Pair.of(timeFromStart, atom(new SchedulingEvent.ResumeActivity<>(UUID.randomUUID().toString(), activity, TreePVector.empty()))));
  }

  public void step() {
    if (this.queue.isEmpty()) return;

    // Get the current time, and any events occurring at this time.
    final Duration delta;
    EventGraph<SchedulingEvent<T, Activity, Event>> events;
    {
      final var job = this.queue.poll();
      events = job.getRight();

      delta = job.getKey().minus(this.currentTime);
      while (!this.queue.isEmpty() && this.queue.peek().getKey().equals(job.getKey())) {
        events = EventGraph.concurrently(events, queue.poll().getValue());
      }
    }

    // Step up to the new time.
    this.now = this.now.wait(delta);
    this.currentTime = this.currentTime.plus(delta);

    // React to the events scheduled at this time.
    final var result = events.evaluate(this.reactor).apply(this.now);
    this.now = result.getLeft();
    final var newJobs = result.getRight();

    // Accumulate the freshly scheduled items into our scheduling timeline.
    for (final var entry : newJobs.entrySet()) {
      final var activityId = entry.getKey();
      final var rule = entry.getValue();
      if (rule instanceof ScheduleItem.Defer) {
        final var duration = ((ScheduleItem.Defer<T, Activity, Event>) rule).duration;
        final var activityType = ((ScheduleItem.Defer<T, Activity, Event>) rule).activityType;
        final var milestones = ((ScheduleItem.Defer<T, Activity, Event>) rule).milestones;
        this.queue.add(Pair.of(duration.plus(this.currentTime), EventGraph.atom(new SchedulingEvent.ResumeActivity<>(activityId, activityType, milestones))));
      } else if (rule instanceof ScheduleItem.OnCompletion) {
        final var waitId = ((ScheduleItem.OnCompletion<T, Activity, Event>) rule).waitOn;
        final var activityType = ((ScheduleItem.OnCompletion<T, Activity, Event>) rule).activityType;
        final var milestones = ((ScheduleItem.OnCompletion<T, Activity, Event>) rule).milestones;
        if (this.completed.contains(waitId)) {
          this.queue.add(Pair.of(this.currentTime, EventGraph.atom(new SchedulingEvent.ResumeActivity<>(activityId, activityType, milestones))));
        } else {
          this.conditioned.computeIfAbsent(waitId, k -> new HashSet<>()).add(Triple.of(activityId, activityType, milestones));
        }
      } else if (rule instanceof ScheduleItem.Complete) {
        this.completed.add(activityId);

        final var conditionedActivities = this.conditioned.remove(entry.getKey());
        if (conditionedActivities == null) continue;

        for (final var conditionedTask : conditionedActivities) {
          final var conditionedId = conditionedTask.getLeft();
          final var activityType = conditionedTask.getMiddle();
          final var milestones = conditionedTask.getRight();
          this.queue.add(Pair.of(this.currentTime, EventGraph.atom(new SchedulingEvent.ResumeActivity<>(conditionedId, activityType, milestones))));
        }
      }
    }
  }

  public boolean hasMoreJobs() {
    return !this.queue.isEmpty();
  }

  public Time<T, Event> getCurrentTime() {
    return this.now;
  }

  public Duration getElapsedTime() {
    return this.currentTime;
  }

  public String getDebugTrace() {
    final var builder = new StringBuilder();

    var durationFromStart = Duration.ZERO;
    for (final var point : this.now.evaluate(new EventGraphProjection<>())) {
      durationFromStart = durationFromStart.plus(point.getKey());
      builder.append(String.format("%10s: %s\n", durationFromStart, point.getValue()));
    }
    for (final var point : this.queue) {
      builder.append(String.format("%10s: %s\n", point.getKey(), point.getValue()));
    }

    return builder.toString();
  }
}
