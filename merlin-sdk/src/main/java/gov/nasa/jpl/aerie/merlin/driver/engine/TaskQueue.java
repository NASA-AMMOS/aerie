package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;

public final class TaskQueue<Task> {
  // A time-ordered queue of all scheduled tasks.
  private final PriorityQueue<Pair<Duration, Task>> queue = new PriorityQueue<>(Comparator.comparing(Pair::getLeft));

  // The elapsed simulation time.
  private Duration elapsedTime = Duration.ZERO;

  public void deferTo(final Duration resumptionTime, final Task task) {
    if (resumptionTime.shorterThan(this.elapsedTime)) {
      throw new RuntimeException("Cannot schedule a task in the past");
    }

    this.queue.add(Pair.of(resumptionTime, task));
  }

  private Optional<Duration> nextStepTime(final Duration maximum) {
    // Is there a pending job before the maximum?
    if (!this.queue.isEmpty()) {
      final var nextJobTime = this.queue.peek().getLeft();
      if (nextJobTime.noLongerThan(maximum)) {
        return Optional.of(nextJobTime);
      }
    }

    // Do we at least need to step forward in time?
    if (this.elapsedTime.shorterThan(maximum)) {
      return Optional.of(maximum);
    }

    // There's nothing to do.
    return Optional.empty();
  }

  public <$Timeline> Optional<TaskFrame<$Timeline, Task>>
  popNextFrame(final History<$Timeline> tip, final Duration maximum) {
    return nextStepTime(maximum).map(nextJobTime -> {
      // Step up to the next job time.
      final var resumptionTime = tip.wait(nextJobTime.minus(this.elapsedTime));
      this.elapsedTime = nextJobTime;

      // Extract any tasks at this time.
      return TaskFrame.of(resumptionTime, builder -> {
        final var iter = this.queue.iterator();

        if (!iter.hasNext()) return;
        var entry = iter.next();

        while (entry.getLeft().noLongerThan(nextJobTime)) {
          iter.remove();

          builder.signal(entry.getRight());

          if (!iter.hasNext()) break;
          entry = iter.next();
        }
      });
    });
  }

  public interface Executor<$Timeline, Task> {
    History<$Timeline> execute(Duration delta, TaskFrame<$Timeline, Task> frame);
  }

  public <$Timeline> History<$Timeline>
  consumeUpTo(final Duration maximum, final History<$Timeline> startTime, final Executor<$Timeline, Task> executor) {
    var elapsedTime = this.getElapsedTime();
    var now = startTime;
    var frame = this.popNextFrame(now, maximum);

    while (frame.isPresent()) {
      final var nextTime = this.getElapsedTime();
      final var delta = nextTime.minus(elapsedTime);

      elapsedTime = nextTime;
      now = executor.execute(delta, frame.get());
      frame = this.popNextFrame(now, maximum);
    }

    return now;
  }

  public String getDebugTrace() {
    final var builder = new StringBuilder();

    @SuppressWarnings("unchecked")
    final var x = (Pair<Duration, Task>[]) this.queue.toArray(new Pair[0]);
    Arrays.sort(x);

    for (final var entry : x) {
      builder.append(String.format("%10s: %s\n", entry.getLeft(), entry.getRight()));
    }

    return builder.toString();
  }

  public Duration getElapsedTime() {
    return this.elapsedTime;
  }

  public Optional<Duration> getNextJobTime() {
    return (this.queue.isEmpty())
        ? Optional.empty()
        : Optional.of(this.queue.peek().getLeft());
  }
}
