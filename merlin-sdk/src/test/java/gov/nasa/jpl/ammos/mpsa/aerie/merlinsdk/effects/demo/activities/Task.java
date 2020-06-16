package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.PMap;

public interface Task<T, Event> {
  Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>> apply(Time<T, Event> time);
}
