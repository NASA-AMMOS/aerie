package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;

import java.util.Objects;
import java.util.Optional;

public final class TaskInfo<$Timeline> {
  public final String id;
  public final Optional<String> parent;
  public boolean isDaemon = false;

  // The remaining work to perform.
  public Task<$Timeline> task;

  // The task type and arguments used to instantiate this task.
  // Empty if this is an anonymous task.
  public final Optional<SerializedActivity> specification;

  // The elapsed time when this task was first resumed.
  public Optional<Duration> startTime = Optional.empty();
  // The elapsed time when this task reported completion.
  public Optional<Duration> endTime = Optional.empty();

  public TaskInfo(
      final String id,
      final Optional<String> parent,
      final Task<$Timeline> task,
      final Optional<SerializedActivity> specification)
  {
    this.id = Objects.requireNonNull(id);
    this.parent = Objects.requireNonNull(parent);
    this.task = Objects.requireNonNull(task);
    this.specification = Objects.requireNonNull(specification);
  }

  public Optional<Window> getWindow() {
    return this.startTime.flatMap(startTime -> {
      return this.endTime.map(endTime -> {
        return Window.between(startTime, endTime);
      });
    });
  }

  public TaskStatus<$Timeline> step(final Duration atTime, final Scheduler<$Timeline> scheduler) {
    if (this.startTime.isEmpty()) this.startTime = Optional.of(atTime);

    final var status = this.task.step(scheduler);

    status.match(new TaskStatus.Visitor<$Timeline, Void>() {
      @Override
      public Void completed() {
        TaskInfo.this.endTime = Optional.of(atTime);
        TaskInfo.this.task = null;
        return null;
      }

      @Override public Void delayed(final Duration delay) { return null; }
      @Override public Void awaiting(final String target) { return null; }
      @Override public Void awaiting(final Condition<? super $Timeline> condition) { return null; }
    });

    return status;
  }
}
