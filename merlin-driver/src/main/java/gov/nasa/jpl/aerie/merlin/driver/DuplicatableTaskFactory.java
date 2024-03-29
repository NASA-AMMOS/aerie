package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.concurrent.Executor;

public record DuplicatableTaskFactory<Output>(
    ActivityDirectiveId directiveId,
    TaskFactory<Output> task,
    Topic<ActivityDirectiveId> activityTopic,
    Executor executor) implements Task<Output> {

  @Override
  public TaskStatus<Output> step(final Scheduler scheduler) {
    scheduler.emit(directiveId, activityTopic);
    return task.create(executor).step(scheduler);
  }

  @Override
  public Task<Output> duplicate(Executor executor) {
    return new DuplicatableTaskFactory<>(directiveId, task, activityTopic, executor);
  }
}
