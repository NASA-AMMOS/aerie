package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Optional;

public sealed interface Action {
  TaskFactory<Unit, ?> toTask(Topic<ActivityInstanceId> activityTopic);
  Optional<Duration> getDuration();

  /** An un-named event. */
  record AnonymousEvent(Duration duration) implements Action {
    @Override
    public TaskFactory<Unit, Unit> toTask(final Topic<ActivityInstanceId> activityTopic) {
      return $ -> Task.delaying(this.duration);
    }

    @Override
    public Optional<Duration> getDuration() {
      return Optional.of(this.duration);
    }
  }

  /** A named event. */
  record Event(ActivityInstanceId id, Duration duration) implements Action {
    public TaskFactory<Unit, Unit> toTask(final Topic<ActivityInstanceId> activityTopic) {
      return $ -> Task.<Unit>delaying(this.duration).butFirst(Task.emitting(this.id, activityTopic));
    }

    @Override
    public Optional<Duration> getDuration() {
      return Optional.of(this.duration);
    }
  }

  /** A simulation directive. */
  record Directive<Input, Output>(ActivityInstanceId id, Input input, TaskFactory<Input, Output> task) implements Action {
    public TaskFactory<Unit, Output> toTask(final Topic<ActivityInstanceId> activityTopic) {
      return executor -> Task.compose(
          Task.completed(this.input),
          Task.calling(this.task.butFirst(Task.emitting(this.id, activityTopic))));
    }

    @Override
    public Optional<Duration> getDuration() {
      return Optional.empty();
    }
  }
}
