package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.TreePVector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class ReplayingSimulationEngine<T, Activity, Event> {
  private final PriorityQueue<Pair<Duration, Task<T, Activity, Event>>> queue = new PriorityQueue<>(Comparator.comparing(Pair::getKey));
  private final Set<String> completed = new HashSet<>();
  private final Map<String, Set<Task<T, Activity, Event>>> conditioned = new HashMap<>();

  private final ActivityReactor<T, Activity, Event> reactor;

  private Time<T, Event> now;
  private Duration elapsedTime = Duration.ZERO;

  public ReplayingSimulationEngine(
      final Time<T, Event> startTime,
      final BiConsumer<ReactionContext<T, Activity, Event>, Activity> executor
  ) {
    this.reactor = new ActivityReactor<>(executor);
    this.now = startTime;
  }

  public void enqueue(final Duration timeFromStart, final Activity activity) {
    this.queue.add(Pair.of(timeFromStart, this.reactor.atom(new ResumeActivityEvent<>(UUID.randomUUID().toString(), activity, TreePVector.empty()))));
  }

  public void runFor(final long quantity, final TimeUnit units) {
    this.runFor(Duration.of(quantity, units));
  }

  public void runFor(final Duration duration) {
    final var endTime = this.elapsedTime.plus(duration);
    while (!endTime.shorterThan(this.elapsedTime)) {
      // If there are no jobs remaining, or the next job is after the end time, simply step up to the end time.
      if (this.queue.isEmpty() || endTime.shorterThan(this.queue.peek().getKey())) {
        this.now = this.now.wait(endTime.minus(this.elapsedTime));
        this.elapsedTime = endTime;
        break;
      }

      // Step up to, and perform, the next job.
      this.step();
    }
  }

  public void step() {
    if (this.queue.isEmpty()) return;
    final var nextJobTime = this.queue.peek().getKey();

    // Collect all of the tasks associated with the next time point.
    var tasks = this.queue.poll().getValue();
    while (!this.queue.isEmpty() && this.queue.peek().getKey().equals(nextJobTime)) {
      tasks = this.reactor.concurrently(tasks, this.queue.poll().getValue());
    }

    // Step up to the next job time, and perform the job.
    this.now = this.now.wait(nextJobTime.minus(this.elapsedTime));
    this.elapsedTime = nextJobTime;

    this.react(tasks);
  }

  private void react(final Task<T, Activity, Event> task) {
    // React to the events scheduled at this time.
    final var result = task.apply(this.now);
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

        this.queue.add(Pair.of(this.elapsedTime.plus(duration), this.reactor.atom(new ResumeActivityEvent<>(activityId, activityType, milestones))));
      } else if (rule instanceof ScheduleItem.OnCompletion) {
        final var waitId = ((ScheduleItem.OnCompletion<T, Activity, Event>) rule).waitOn;
        final var activityType = ((ScheduleItem.OnCompletion<T, Activity, Event>) rule).activityType;
        final var milestones = ((ScheduleItem.OnCompletion<T, Activity, Event>) rule).milestones;

        final var resumption = this.reactor.atom(new ResumeActivityEvent<>(activityId, activityType, milestones));
        if (this.completed.contains(waitId)) {
          this.queue.add(Pair.of(this.elapsedTime, resumption));
        } else {
          this.conditioned.computeIfAbsent(waitId, k -> new HashSet<>()).add(resumption);
        }
      } else if (rule instanceof ScheduleItem.Complete) {
        this.completed.add(activityId);

        final var conditionedActivities = this.conditioned.remove(entry.getKey());
        if (conditionedActivities == null) continue;

        for (final var conditionedTask : conditionedActivities) {
          this.queue.add(Pair.of(this.elapsedTime, conditionedTask));
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
    return this.elapsedTime;
  }

  public String getDebugTrace() {
    final var builder = new StringBuilder();

    builder.append(this.now.getDebugTrace());
    for (final var point : this.queue) {
      builder.append(String.format("%10s: %s\n", point.getKey(), point.getValue()));
    }

    return builder.toString();
  }
}
