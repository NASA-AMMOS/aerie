package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public record DuplicatableTaskFactory<Output>(
    Consumer<Scheduler> prelude,
    TaskFactory<Output> task,
    Executor executor
) implements Task<Output> {

  @Override
  public TaskStatus<Output> step(final Scheduler scheduler) {
    this.prelude.accept(scheduler);
    return this.task.create(this.executor).step(scheduler);
  }

  @Override
  public Task<Output> duplicate(Executor executor) {
    return new DuplicatableTaskFactory<>(this.prelude, this.task, executor);
  }
}
