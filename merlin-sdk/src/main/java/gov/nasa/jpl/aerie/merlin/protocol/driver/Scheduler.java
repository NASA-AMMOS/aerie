package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public interface Scheduler {
  <State> State get(Query<?, State> query);

  <Event> void emit(Event event, Query<? super Event, ?> query);

  TaskIdentifier spawn(Task task);
  TaskIdentifier spawn(String type, Map<String, SerializedValue> arguments);

  TaskIdentifier defer(Duration delay, Task task);
  TaskIdentifier defer(Duration delay, String type, Map<String, SerializedValue> arguments);

  interface TaskIdentifier {}
}
