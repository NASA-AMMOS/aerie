package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;

public interface Context {
  <CellType> CellType ask(Query<?, ?, CellType> query);
  <Event> void emit(Event event, Query<?, Event, ?> query);

  String spawn(Runnable task);
  String spawn(String type, Map<String, SerializedValue> arguments);

  String defer(Duration duration, Runnable task);
  String defer(Duration duration, String type, Map<String, SerializedValue> arguments);

  void delay(Duration duration);
  void waitFor(String id);
  void waitUntil(Condition condition);
}
