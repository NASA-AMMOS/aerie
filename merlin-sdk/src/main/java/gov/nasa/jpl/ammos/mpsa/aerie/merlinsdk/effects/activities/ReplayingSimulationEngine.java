package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.TreePVector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

public final class ReplayingSimulationEngine<T, Activity, Event> {
  private final PriorityQueue<Pair<Duration, ResumeActivityEvent<T, Activity, Event>>> queue = new PriorityQueue<>(Comparator.comparing(Pair::getKey));
  private final Set<String> completed = new HashSet<>();
  private final Map<String, Set<ResumeActivityEvent<T, Activity, Event>>> conditioned = new HashMap<>();

  private final ActivityReactor<T, Activity, Event> reactor;

  private History<T, Event> currentHistory;
  private Duration elapsedTime = Duration.ZERO;

  public ReplayingSimulationEngine(
      final History<T, Event> initialHistory,
      final ActivityExecutor<T, Activity, Event> executor
  ) {
    this.reactor = new ActivityReactor<>(executor);
    this.currentHistory = initialHistory;
  }

  public void enqueue(final Duration timeFromStart, final Activity activity) {
    // TODO: It is somewhat a code smell that we have to conjure our IDs randomly from the ether.
    //   Figure out a better way to identify activity instances.
    //   Make sure we handle the cases in ReactionContextImpl, too.
    this.enqueue(timeFromStart, UUID.randomUUID().toString(), activity);
  }

  public void enqueue(final Duration timeFromStart, final String activityId, final Activity activity) {
    this.queue.add(Pair.of(timeFromStart, new ResumeActivityEvent<>(activityId, activity, TreePVector.empty())));
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
    final var nextJobTime = this.queue.peek().getKey();

    // Collect all of the tasks associated with the next time point.
    var tasks = this.reactor.atom(this.queue.poll().getValue());
    while (!this.queue.isEmpty() && this.queue.peek().getKey().equals(nextJobTime)) {
      tasks = this.reactor.concurrently(tasks, this.reactor.atom(this.queue.poll().getValue()));
    }

    // Step up to the next job time, and perform the job.
    this.currentHistory = this.currentHistory.wait(nextJobTime.minus(this.elapsedTime));
    this.elapsedTime = nextJobTime;

    this.react(tasks);
  }

  private void react(final Task<T, Activity, Event> task) {
    // React to the events scheduled at this time.
    final var result = task.apply(this.currentHistory);
    this.currentHistory = result.getLeft();
    final var newJobs = result.getRight();

    // Accumulate the freshly scheduled items into our scheduling timeline.
    for (final var entry : newJobs.entrySet()) {
      final var activityId = entry.getKey();
      final var rule = entry.getValue();

      if (rule instanceof ScheduleItem.Defer) {
        final var duration = ((ScheduleItem.Defer<T, Activity, Event>) rule).duration;
        final var activityType = ((ScheduleItem.Defer<T, Activity, Event>) rule).activityType;
        final var milestones = ((ScheduleItem.Defer<T, Activity, Event>) rule).milestones;

        this.queue.add(Pair.of(this.elapsedTime.plus(duration), new ResumeActivityEvent<>(activityId, activityType, milestones)));
      } else if (rule instanceof ScheduleItem.OnCompletion) {
        final var waitId = ((ScheduleItem.OnCompletion<T, Activity, Event>) rule).waitOn;
        final var activityType = ((ScheduleItem.OnCompletion<T, Activity, Event>) rule).activityType;
        final var milestones = ((ScheduleItem.OnCompletion<T, Activity, Event>) rule).milestones;

        final var resumption = new ResumeActivityEvent<>(activityId, activityType, milestones);
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
