package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;

import java.util.Objects;
import java.util.function.Consumer;

public final class OneShotTask<$Timeline> implements Task<$Timeline> {
  private final Consumer<Scheduler<$Timeline>> task;
  private boolean isTerminated = false;

  public OneShotTask(final Consumer<Scheduler<$Timeline>> task) {
    this.task = Objects.requireNonNull(task);
  }

  @Override
  public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
    if (this.isTerminated) throw new IllegalStateException("step() called on a terminated task");

    this.task.accept(scheduler);

    this.isTerminated = true;
    return TaskStatus.completed();
  }

  @Override
  public void reset() {
    this.isTerminated = true;
  }
}
