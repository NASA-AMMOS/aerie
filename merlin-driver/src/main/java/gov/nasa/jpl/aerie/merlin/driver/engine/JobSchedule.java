package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SubInstantDuration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.util.Comparator.comparing;

public final class JobSchedule<JobRef, TimeRef extends SchedulingInstant> {
  private static boolean debug = false;
  /** The scheduled time for each upcoming job. */
  private final Map<JobRef, Pair<TimeRef, Long>> scheduledJobs = new HashMap<>();

  /** A time-ordered queue of all tasks whose resumption time is concretely known. */
  @DerivedFrom("scheduledJobs")
  private final ConcurrentSkipListSet<Pair<Pair<TimeRef, Long>, JobRef>> queue = new ConcurrentSkipListSet<>(comparing(Pair::getLeft));
  private long JOB_SEQUENCE_NUMBER = Long.MIN_VALUE;

  private long DEBUG_scheduleCalls = 0;
  private long DEBUG_removed = 0;
  private long DEBUG_tasksScheduled = 0;
  private long DEBUG_conditionsScheduled = 0;
  private long DEBUG_resourcesScheduled = 0;
  private long DEBUG_tasksRemoved = 0;
  private long DEBUG_conditionsRemoved = 0;
  private long DEBUG_resourcesRemoved = 0;
  public void schedule(final JobRef job, final TimeRef time) {
    final var unambiguousTime = Pair.of(time, ++JOB_SEQUENCE_NUMBER);
    final var oldTime = this.scheduledJobs.put(job, unambiguousTime);

    if (oldTime != null) {
      // DEBUG - START
      if (debug) {
        ++DEBUG_removed;
        switch (time.priority()) {
          case Tasks -> ++DEBUG_tasksRemoved;
          case Conditions -> ++DEBUG_conditionsRemoved;
          case Resources -> ++DEBUG_resourcesRemoved;
        }
      }
      // DEBUG - END
      this.queue.remove(Pair.of(oldTime, job));
    }
    // DEBUG - START
    if (debug) {
      ++DEBUG_scheduleCalls;
      switch (time.priority()) {
        case Tasks -> ++DEBUG_tasksScheduled;
        case Conditions -> ++DEBUG_conditionsScheduled;
        case Resources -> ++DEBUG_resourcesScheduled;
      }
      if (DEBUG_scheduleCalls % 10_000 == 0) {
        System.out.printf("Schedule statistics:%n");
        System.out.printf("            %-10s | %-10s | %-10s | %-10s%n", "Total", "Tasks", "Conditions", "Resources");
        System.out.printf(
            "Scheduled:  %10d | %10d | %10d | %10d%n",
            DEBUG_scheduleCalls,
            DEBUG_tasksScheduled,
            DEBUG_conditionsScheduled,
            DEBUG_resourcesScheduled);
        System.out.printf(
            "%% of Sch.:  %9.1f%% | %9.1f%% | %9.1f%% | %9.1f%%%n",
            100.0,
            100.0 * DEBUG_tasksScheduled / DEBUG_scheduleCalls,
            100.0 * DEBUG_conditionsScheduled / DEBUG_scheduleCalls,
            100.0 * DEBUG_resourcesScheduled / DEBUG_scheduleCalls);
        System.out.printf(
            "Removed:    %10d | %10d | %10d | %10d%n",
            DEBUG_removed,
            DEBUG_tasksRemoved,
            DEBUG_conditionsRemoved,
            DEBUG_resourcesRemoved);
        System.out.printf(
            "%% of Rem.:  %9.1f%% | %9.1f%% | %9.1f%% | %9.1f%%%n",
            100.0,
            100.0 * DEBUG_tasksRemoved / DEBUG_removed,
            100.0 * DEBUG_conditionsRemoved / DEBUG_removed,
            100.0 * DEBUG_resourcesRemoved / DEBUG_removed);
        System.out.printf(
            "%% of Cat.:  %9.1f%% | %9.1f%% | %9.1f%% | %9.1f%%%n",
            100.0 * DEBUG_removed / DEBUG_scheduleCalls,
            100.0 * DEBUG_tasksRemoved / DEBUG_tasksScheduled,
            100.0 * DEBUG_conditionsRemoved / DEBUG_conditionsScheduled,
            100.0 * DEBUG_resourcesRemoved / DEBUG_resourcesScheduled);
      }
    }
    // DEBUG - END
    this.queue.add(Pair.of(unambiguousTime, job));
  }

  public void unschedule(final JobRef job) {
    final var oldTime = this.scheduledJobs.remove(job);

    if (oldTime != null) this.queue.remove(Pair.of(oldTime, job));
  }

  /** Returns the offset time of the next set of job in the queue. */
  public SubInstantDuration timeOfNextJobs() {
    if (this.queue.isEmpty()) return SubInstantDuration.MAX_VALUE;
    final var time = this.queue.first().getKey().getLeft();
    final JobRef jobRef = this.queue.first().getValue();
    if (jobRef instanceof SimulationEngine.JobId.ResourceJobId) {
      return new SubInstantDuration(time.project(), Integer.MAX_VALUE);
    }
    return new SubInstantDuration(time.project(), 0);
  }

  public Batch<JobRef> extractNextJobs(final Duration maximumTime) {
    if (this.queue.isEmpty()) return new Batch<>(maximumTime, Collections.emptySet());

    final var time = this.queue.first().getKey().getLeft();
    if (time.project().longerThan(maximumTime)) {
      return new Batch<>(maximumTime, Collections.emptySet());
    }

    // Ready all tasks at the soonest task time.
    final var readyJobs = new HashSet<JobRef>();
    while (!queue.isEmpty()) {
      final var entry = this.queue.first();
      if (entry.getKey().getLeft().compareTo(time) > 0) break;

      this.scheduledJobs.remove(entry.getRight());
      this.queue.pollFirst();  // removes first entry

      readyJobs.add(entry.getRight());
    }

    return new Batch<>(time.project(), readyJobs);
  }

  public Optional<TimeRef> min() {
    if (this.queue.isEmpty()) return Optional.empty();
    return Optional.of(queue.first().getKey().getLeft());
  }

  public void clear() {
    this.scheduledJobs.clear();
    this.queue.clear();
  }

  public record Batch<JobRef>(Duration offsetFromStart, Set<JobRef> jobs) {}
}
