package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Map;

public interface Context<$Schema> {
  History<? extends $Schema> now();

  <Event> void emit(Event event, Query<? super $Schema, Event, ?> query);
  <Spec> String spawn(Spec spec, TaskSpecType<? super $Schema, Spec> type);
  <Spec> String defer(Duration duration, Spec spec, TaskSpecType<? super $Schema, Spec> type);

  String spawn(String type, Map<String, SerializedValue> arguments);

  void delay(Duration duration);
  void waitFor(String id);
  void waitUntil(Condition<$Schema> condition);
}
