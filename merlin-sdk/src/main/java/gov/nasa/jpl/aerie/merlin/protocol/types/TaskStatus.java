package gov.nasa.jpl.aerie.merlin.protocol.types;

import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;

import java.util.Map;

public sealed interface TaskStatus {
  record Completed() implements TaskStatus {}

  record Delayed(Duration delay) implements TaskStatus {}

  record AwaitingTask(Task task) implements TaskStatus {}

  record AwaitingDirective(String type, Map<String, SerializedValue> arguments) implements TaskStatus {}

  record AwaitingCondition(Condition condition) implements TaskStatus {}


  static Completed completed() {
    return new Completed();
  }

  static Delayed delayed(final Duration delay) {
    return new Delayed(delay);
  }

  static AwaitingTask awaiting(final Task task) {
    return new AwaitingTask(task);
  }

  static AwaitingDirective awaiting(final String type, final Map<String, SerializedValue> arguments) {
    return new AwaitingDirective(type, arguments);
  }

  static AwaitingCondition awaiting(final Condition condition) {
    return new AwaitingCondition(condition);
  }
}
