package gov.nasa.jpl.aerie.merlin.protocol.types;

import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;

public sealed interface TaskStatus<Return> {
  record Completed<Return>(Return returnValue) implements TaskStatus<Return> {}

  record Delayed<Return>(Duration delay) implements TaskStatus<Return> {}

  record AwaitingTask<Return>(String target) implements TaskStatus<Return> {}

  record AwaitingCondition<Return>(Condition condition) implements TaskStatus<Return> {}


  static <Return> Completed<Return> completed(final Return returnValue) {
    return new Completed<>(returnValue);
  }

  static <Return> Delayed<Return> delayed(final Duration delay) {
    return new Delayed<>(delay);
  }

  static <Return> AwaitingTask<Return> awaiting(final String id) {
    return new AwaitingTask<>(id);
  }

  static <Return> AwaitingCondition<Return> awaiting(final Condition condition) {
    return new AwaitingCondition<>(condition);
  }
}
