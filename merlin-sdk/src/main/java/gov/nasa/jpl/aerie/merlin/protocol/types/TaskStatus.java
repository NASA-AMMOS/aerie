package gov.nasa.jpl.aerie.merlin.protocol.types;

import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;

public sealed interface TaskStatus {
  record Completed() implements TaskStatus {}

  record Delayed(Duration delay) implements TaskStatus {}

  record AwaitingTask(String target) implements TaskStatus {}

  record AwaitingCondition(Condition condition) implements TaskStatus {}


  static Completed completed() {
    return new Completed();
  }

  static Delayed delayed(final Duration delay) {
    return new Delayed(delay);
  }

  static AwaitingTask awaiting(final String id) {
    return new AwaitingTask(id);
  }

  static AwaitingCondition awaiting(final Condition condition) {
    return new AwaitingCondition(condition);
  }
}
