package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface TaskScheduler<T, Event> {
  void spawn(History<T, Event> forkPoint, SimulationTask<T, Event> task);
  void defer(Duration delay, SimulationTask<T, Event> task);
  void await(String taskToAwait, SimulationTask<T, Event> task);
  void complete(String taskId);
}
