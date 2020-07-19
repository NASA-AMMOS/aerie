package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;

import java.util.function.Consumer;

public interface SimulationTask<T, Event> {
  String getId();

  History<T, Event> runFrom(History<T, Event> history, Consumer<ScheduleItem<T, Event>> scheduler);
}
