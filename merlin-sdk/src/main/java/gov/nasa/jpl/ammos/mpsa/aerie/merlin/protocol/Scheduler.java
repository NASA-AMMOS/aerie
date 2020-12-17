package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Map;

public interface Scheduler<$Timeline> {
  History<$Timeline> now();
  <Event> void emit(Event event, Query<? super $Timeline, Event, ?> query);
  <Spec> String spawn(Spec spec, TaskSpecType<? super $Timeline, Spec> type);

  String spawn(String type, Map<String, SerializedValue> arguments);

  <Spec> String defer(Duration delay, Spec spec, TaskSpecType<? super $Timeline, Spec> type);
}
