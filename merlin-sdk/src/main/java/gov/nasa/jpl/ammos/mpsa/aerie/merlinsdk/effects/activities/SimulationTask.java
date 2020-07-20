package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface SimulationTask<T, Event> {
  String getId();

  Pair<History<T, Event>, ? extends Map<String, ScheduleItem<T, Event>>> runFrom(History<T, Event> history);
}
