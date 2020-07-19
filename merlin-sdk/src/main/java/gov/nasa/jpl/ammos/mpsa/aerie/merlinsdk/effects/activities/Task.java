package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.PMap;

public interface Task<T, Event, Activity> {
  Pair<History<T, Event>, PMap<String, ScheduleItem<Activity>>> apply(History<T, Event> history);
}
