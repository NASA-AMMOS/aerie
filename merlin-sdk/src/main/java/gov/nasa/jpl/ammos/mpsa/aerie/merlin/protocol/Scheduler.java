package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Map;

public interface Scheduler<$Timeline> {
  History<$Timeline> now();
  <Solution> Solution ask(SolvableDynamics<Solution, ?> resource, Duration offset);

  <Event> void emit(Event event, Query<? super $Timeline, Event, ?> query);
  String spawn(String type, Map<String, SerializedValue> arguments);
  String defer(Duration delay, String type, Map<String, SerializedValue> arguments);
}
