package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface ReactionContext<T, Event, Activity> {
  History<T, Event> now();

  void emit(Event event);
  void delay(Duration duration);
  // TODO: parametrize `ReactionContext` over an opaque `ActivityHandle` type so that ID types are not leaked,
  //   and so that we can remove `waitForActivity` (by moving it onto an interface implemented by the handle type).
  String spawn(Activity activity);
  String spawnAfter(Duration delay, Activity activity);
  void waitForActivity(String activityId);
  void waitForChildren();
  default void call(final Activity activity) {
    waitForActivity(spawn(activity));
  }
}
