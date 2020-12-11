package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Map;

public interface  Scheduler<$Timeline> {
  History<$Timeline> now();
  <Event> void emit(Event event, Query<? super $Timeline, Event, ?> query);
  <Spec> String spawn(Spec spec, TaskSpecType<? super $Timeline, Spec> type);

  // This is marked deprecated because its intended use is to enable the
  // exercising of the spawn mechanism. Similarly to the Context interface
  // it shall be removed when a task can be instantiated from within the
  // Module where spawn is called.
  @Deprecated
  String spawn(String type, Map<String, SerializedValue> arguments);

  <Spec> String defer(Duration delay, Spec spec, TaskSpecType<? super $Timeline, Spec> type);
}
