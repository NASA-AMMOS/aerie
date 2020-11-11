package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface Context<$Timeline, Event, Activity> {
  History<? extends $Timeline, ?> now();

  void emit(Event event);
  <T> T ask(Resource<? super History<? extends $Timeline, ?>, T> resource);

  String spawn(Activity activity);
  String defer(Duration duration, Activity activity);
  void delay(Duration duration);
  void waitFor(String id);

  default void call(final Activity activity) {
    this.waitFor(this.spawn(activity));
  }

  default String defer(final long quantity, final Duration unit, final Activity activity) {
    return this.defer(unit.times(quantity), activity);
  }

  default void delay(long quantity, Duration unit) {
    this.delay(unit.times(quantity));
  }
}
