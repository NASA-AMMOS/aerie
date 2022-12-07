package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Optional;

public sealed interface Action {
  Task<Unit, Unit> toTask(Topic<ActivityInstanceId> activityTopic);
  Optional<Duration> getDuration();

  /** An un-named event. */
  record AnonymousEvent(Duration duration) implements Action {
    @Override
    public Task<Unit, Unit> toTask(final Topic<ActivityInstanceId> activityTopic) {
      return Task.delaying(this.duration);
    }

    @Override
    public Optional<Duration> getDuration() {
      return Optional.of(this.duration);
    }
  }

  /** A named event. */
  record Event(ActivityInstanceId id, Duration duration) implements Action {
    public Task<Unit, Unit> toTask(final Topic<ActivityInstanceId> activityTopic) {
      return ($, $$) -> TaskStatus.calling(
          Unit.UNIT,
          executor -> (scheduler, input) -> {
            scheduler.emit(this.id, activityTopic);
            return TaskStatus.delayed(this.duration, Task.completed(input));
          },
          Task.identity());
    }

    @Override
    public Optional<Duration> getDuration() {
      return Optional.of(this.duration);
    }
  }

  /** A simulation directive. */
  record Directive<Input, Output>(ActivityInstanceId id, Input input, TaskFactory<Input, Output> task) implements Action {
    public Task<Unit, Unit> toTask(final Topic<ActivityInstanceId> activityTopic) {
      return ($, $$) -> TaskStatus.calling(
          this.input,
          executor -> (scheduler, input) -> {
            scheduler.emit(this.id, activityTopic);
            return this.task.create(executor).step(scheduler, input);
          },
          Task.completed(Unit.UNIT));
    }

    @Override
    public Optional<Duration> getDuration() {
      return Optional.empty();
    }
  }
}
