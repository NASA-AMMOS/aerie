package gov.nasa.jpl.aerie.merlin.protocol.types;

import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;

public sealed interface TaskStatus<Return> {
  record Completed<Return>(Return returnValue) implements TaskStatus<Return> {}

  record Delayed<Return>(Duration delay, Task<Return> continuation) implements TaskStatus<Return> {}

  record CallingTask<Return>(TaskFactory<?> child, Task<Return> continuation, boolean tailCall)
    implements TaskStatus<Return> {}

  record AwaitingCondition<Return>(Condition condition, Task<Return> continuation) implements TaskStatus<Return> {}


  static <Return> Completed<Return> completed(final Return returnValue) {
    return new Completed<>(returnValue);
  }

  static <Return> Delayed<Return> delayed(final Duration delay, final Task<Return> continuation) {
    return new Delayed<>(delay, continuation);
  }

  static <Return> CallingTask<Return> calling(final TaskFactory<?> child, final Task<Return> continuation) {
    return new CallingTask<>(child, continuation, false);
  }

  static <Return> CallingTask<Return> tailCalling(final TaskFactory<?> child, final Task<Return> continuation) {
    return new CallingTask<>(child, continuation, true);
  }

  static <Return> AwaitingCondition<Return> awaiting(final Condition condition, final Task<Return> continuation) {
    return new AwaitingCondition<>(condition, continuation);
  }
}
