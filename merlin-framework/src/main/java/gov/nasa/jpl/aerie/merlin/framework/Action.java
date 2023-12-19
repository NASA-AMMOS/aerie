package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

public sealed interface Action {
  record Emit<Event>(Event event, Topic<Event> topic) implements Action {
    @Override
    public String toString() {
      return "emit(event=" + event + ", topic=" + topic + ")";
    }
  }
  record Yield(TaskStatus<?> taskStatus) implements Action {
    @Override
    public String toString() {
      if (taskStatus instanceof TaskStatus.Completed<?> s) {
        return "Completed(" + s.returnValue().toString() + ")";
      } else if (taskStatus instanceof TaskStatus.Delayed<?> s) {
        return "delay(" + s.delay().toString() + ")";
      } else if (taskStatus instanceof TaskStatus.CallingTask<?> s) {
        return "call(" + s.child().toString() + ")";
      } else if (taskStatus instanceof TaskStatus.AwaitingCondition<?> s) {
        return "waitUntil(" + s.condition().toString() + ")";
      } else {
        throw new Error("Unhandled variant of TaskStatus: " + taskStatus);
      }
    }
  }
  record Spawn() implements Action {}
  record Read(CellId<?> query) implements Action {}
}
