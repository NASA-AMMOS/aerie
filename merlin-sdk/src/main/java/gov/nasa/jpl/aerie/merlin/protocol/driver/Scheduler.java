package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;

public interface Scheduler {
  <State> State get(Query<State> query);

  <Event> void emit(Event event, Topic<Event> topic);

  <Output> String spawn(Task<Output> task);
}
