package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.CompoundCondition;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;

public interface Context<$Schema> {
  History<? extends $Schema> now();

  <Event> void emit(Event event, Query<? super $Schema, Event, ?> query);

  String spawn(String type, Map<String, SerializedValue> arguments);
  String defer(Duration duration, String type, Map<String, SerializedValue> arguments);

  void delay(Duration duration);
  void waitFor(String id);
  void waitUntil(CompoundCondition<?> condition);
}
