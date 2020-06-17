package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface ReactionContext<T, Activity, Event> {
  Time<T, Event> now();

  void emit(Event event);
  void delay(Duration duration);
  String spawn(Activity activity);
  void waitForActivity(String activityId);
  void waitForChildren();
}
