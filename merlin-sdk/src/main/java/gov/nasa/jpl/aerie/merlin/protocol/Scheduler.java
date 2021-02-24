package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;

public interface Scheduler<$Timeline> {
  Checkpoint<$Timeline> now();
  <Event> void emit(Event event, Query<? super $Timeline, Event, ?> query);

  String spawn(String type, Map<String, SerializedValue> arguments);
  String defer(Duration delay, String type, Map<String, SerializedValue> arguments);
}
