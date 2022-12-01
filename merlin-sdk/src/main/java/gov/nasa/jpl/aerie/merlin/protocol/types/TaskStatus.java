package gov.nasa.jpl.aerie.merlin.protocol.types;

import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;

public sealed interface TaskStatus<Output> {
  record Completed<Output>(Output returnValue) implements TaskStatus<Output> {}

  record Delayed<Output>(Duration delay, Task<Unit, Output> continuation) implements TaskStatus<Output> {}

  record CallingTask<Midput, Output>(TaskFactory<Unit, Midput> child, Task<Midput, Output> continuation) implements TaskStatus<Output> {}

  record AwaitingCondition<Output>(Condition condition, Task<Unit, Output> continuation) implements TaskStatus<Output> {}


  static <Output> Completed<Output> completed(final Output returnValue) {
    return new Completed<>(returnValue);
  }

  static <Output> Delayed<Output> delayed(final Duration delay, final Task<Unit, Output> continuation) {
    return new Delayed<>(delay, continuation);
  }

  static <Midput, Output> CallingTask<Midput, Output> calling(final TaskFactory<Unit, Midput> child, final Task<Midput, Output> continuation) {
    return new CallingTask<>(child, continuation);
  }

  static <Output> AwaitingCondition<Output> awaiting(final Condition condition, final Task<Unit, Output> continuation) {
    return new AwaitingCondition<>(condition, continuation);
  }
}
