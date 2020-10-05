package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;

public interface SimulationTask<T, Event> {
  String getId();

  History<T, Event> runFrom(History<T, Event> history, TaskScheduler<T, Event> scheduler);
}
