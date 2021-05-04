package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;

public interface Scheduler<$Timeline> {
  Checkpoint<$Timeline> now();
  <State> State getStateAt(Checkpoint<$Timeline> time, Query<? super $Timeline, ?, State> query);

  <Event, State> void emit(Event event, Query<? super $Timeline, ? super Event, State> query);

  String spawn(Task<$Timeline> task);
  String spawn(String type, Map<String, SerializedValue> arguments);

  String defer(Duration delay, Task<$Timeline> task);
  String defer(Duration delay, String type, Map<String, SerializedValue> arguments);
}
