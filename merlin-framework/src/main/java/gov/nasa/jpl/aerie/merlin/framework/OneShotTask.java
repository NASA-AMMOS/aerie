package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Objects;
import java.util.function.Consumer;

public final class OneShotTask implements Task<Unit> {
  private final Consumer<Scheduler> task;
  private boolean isTerminated = false;

  public OneShotTask(final Consumer<Scheduler> task) {
    this.task = Objects.requireNonNull(task);
  }

  @Override
  public TaskStatus<Unit> step(final Scheduler scheduler) {
    if (this.isTerminated) throw new IllegalStateException("step() called on a terminated task");

    this.task.accept(scheduler);

    this.isTerminated = true;
    return TaskStatus.completed(Unit.UNIT);
  }

  @Override
  public void reset() {
    this.isTerminated = true;
  }
}
