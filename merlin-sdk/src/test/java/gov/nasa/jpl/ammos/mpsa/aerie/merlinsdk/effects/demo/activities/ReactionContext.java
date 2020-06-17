package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

public interface ReactionContext<Event> {
  void emit(Event event);
  void delay(Duration duration);
  String spawn(String activity);
  void waitForActivity(String activityId);
  void waitForChildren();

  <Result> Result as(final Getter<Event, Result> interpreter);

  default void delay(long quantity, TimeUnit units) {
    this.delay(Duration.of(quantity, units));
  }

  @FunctionalInterface
  interface Getter<Event, Result> {
    <T> Result apply(final Querier<T> querier, final Time<T, Event> time);
  }
}
