package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;

import java.util.function.Consumer;

public interface SimulationTask<T, Event> {
  String getId();

  TaskFrame<T, Event> runFrom(History<T, Event> history, Consumer<ScheduleItem<T, Event>> scheduler);
}
