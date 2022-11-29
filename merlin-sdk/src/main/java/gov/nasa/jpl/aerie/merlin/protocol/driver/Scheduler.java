package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

public interface Scheduler {
  <State> State get(CellId<State> cellId);

  <Event> void emit(Event event, Topic<Event> topic);

  void spawn(TaskFactory<Unit, ?> task);
}
