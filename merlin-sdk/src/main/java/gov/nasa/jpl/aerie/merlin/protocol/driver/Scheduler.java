package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;

public interface Scheduler {
  <State> State get(CellId<State> cellId);

  <Event> void emit(Event event, Topic<Event> topic);

  <Output> void spawn(Task<Output> task);
}
