package gov.nasa.jpl.aerie.merlin.driver.retracing.engine.tracing;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

public sealed interface Action<T> {
  record Emit<T, Event>(Event event, Topic<Event> topic) implements Action<T> {

    void apply(Scheduler scheduler) {
      scheduler.emit(event, topic);
    }

    @Override
    public String toString() {
      return "emit(event=" + event + ", topic=" + topic + ")";
    }
  }
  record Yield<T>(TaskStatus<T> taskStatus) implements Action<T> {
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
  record Spawn<T>(InSpan childSpan, TaskFactory<?> child) implements Action<T> {}
}
