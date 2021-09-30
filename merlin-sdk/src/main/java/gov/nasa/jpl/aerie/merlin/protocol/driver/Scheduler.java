package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public interface Scheduler<$Timeline> {
  <State> State get(Query<? super $Timeline, ?, State> query);

  <Event> void emit(Event event, Query<? super $Timeline, ? super Event, ?> query);

  String spawn(Task<$Timeline> task);
  String spawn(String type, Map<String, SerializedValue> arguments);

  String defer(Duration delay, Task<$Timeline> task);
  String defer(Duration delay, String type, Map<String, SerializedValue> arguments);
}
