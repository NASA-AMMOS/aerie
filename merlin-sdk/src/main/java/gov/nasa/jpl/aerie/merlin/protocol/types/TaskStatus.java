package gov.nasa.jpl.aerie.merlin.protocol.types;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;

import java.util.Optional;

public sealed interface TaskStatus {
  record Completed<ReturnType>(Optional<TaskReturnValue<ReturnType>> returnValue) implements TaskStatus {}

  record Delayed(Duration delay) implements TaskStatus {}

  record AwaitingTask(Scheduler.TaskIdentifier target) implements TaskStatus {}

  record AwaitingCondition(Condition condition) implements TaskStatus {}


  static <ReturnType> Completed<ReturnType> completed(final ReturnType returnValue) {
    return new Completed<>(Optional.of(new TaskReturnValue<>(returnValue)));
  }

  static Completed<Void> completed() {
    return new Completed<>(Optional.empty());
  }

  static Delayed delayed(final Duration delay) {
    return new Delayed(delay);
  }

  static AwaitingTask awaiting(final Scheduler.TaskIdentifier id) {
    return new AwaitingTask(id);
  }

  static AwaitingCondition awaiting(final Condition condition) {
    return new AwaitingCondition(condition);
  }
}
