package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

public final class JobSchedule<JobRef, TimeRef extends SchedulingInstant> {
  /** The scheduled time for each upcoming job. */
  private final Map<JobRef, TimeRef> scheduledJobs = new HashMap<>();

  /** A time-ordered queue of all tasks whose resumption time is concretely known. */
  @DerivedFrom("scheduledJobs")
  private final PriorityQueue<Pair<TimeRef, JobRef>> queue = new PriorityQueue<>(Comparator.comparing(Pair::getLeft));

  public void schedule(final JobRef job, final TimeRef time) {
    final var oldTime = this.scheduledJobs.put(job, time);

    if (oldTime != null) this.queue.remove(Pair.of(oldTime, job));
    this.queue.add(Pair.of(time, job));
  }

  public void unschedule(final JobRef job) {
    final var oldTime = this.scheduledJobs.remove(job);

    if (oldTime != null) this.queue.remove(Pair.of(oldTime, job));
  }

  public Batch<JobRef> extractNextJobs(final Duration maximumTime) {
    if (this.queue.isEmpty()) return new Batch<>(maximumTime, Collections.emptySet());

    final var time = this.queue.peek().getKey();
    if (time.project().longerThan(maximumTime)) {
      return new Batch<>(maximumTime, Collections.emptySet());
    }

    // Ready all tasks at the soonest task time.
    final var readyJobs = new HashSet<JobRef>();
    while (true) {
      final var entry = this.queue.peek();
      if (entry == null) break;
      if (entry.getLeft().compareTo(time) > 0) break;

      this.scheduledJobs.remove(entry.getRight());
      this.queue.remove();

      readyJobs.add(entry.getRight());
    }

    return new Batch<>(time.project(), readyJobs);
  }

  public void clear() {
    this.scheduledJobs.clear();
    this.queue.clear();
  }

  public Optional<Duration> peekNextTime() {
    return Optional.ofNullable(this.queue.peek()).map($ -> $.getLeft().offsetFromStart());
  }

  public record Batch<JobRef>(Duration offsetFromStart, Set<JobRef> jobs) {}

  public JobSchedule<JobRef, TimeRef> duplicate() {
    final JobSchedule<JobRef, TimeRef> jobSchedule = new JobSchedule<>();
    jobSchedule.queue.addAll(this.queue);
    jobSchedule.scheduledJobs.putAll(this.scheduledJobs);
    return jobSchedule;
  }
}
