package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Projection;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;

public interface Context {
  // Usable during both initialization & simulation
  <CellType> CellType ask(Query<?, ?, CellType> query);

  // Usable during initialization
  <Event, Effect, CellType>
  Query<?, Event, CellType>
  allocate(
      final Projection<Event, Effect> projection,
      final Applicator<Effect, CellType> applicator);

  // Usable during simulation
  <Event> void emit(Event event, Query<?, Event, ?> query);

  String spawn(Runnable task);
  String spawn(String type, Map<String, SerializedValue> arguments);

  String defer(Duration duration, Runnable task);
  String defer(Duration duration, String type, Map<String, SerializedValue> arguments);

  void delay(Duration duration);
  void waitFor(String id);
  void waitUntil(Condition condition);
}
