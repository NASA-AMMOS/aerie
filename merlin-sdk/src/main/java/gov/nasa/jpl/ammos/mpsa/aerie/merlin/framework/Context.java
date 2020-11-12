package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Set;

public interface Context<$Timeline, Event, Activity> {
  History<$Timeline, ?> now();
  double ask(RealResource<? super History<$Timeline, ?>> resource);
  <T> T ask(DiscreteResource<? super History<$Timeline, ?>, T> resource);

  void emit(Event event);
  String spawn(Activity activity);
  String defer(Duration duration, Activity activity);

  void delay(Duration duration);
  void waitFor(String id);
  void waitFor(RealResource<? super History<$Timeline, ?>> resource, RealCondition condition);
  <T> void waitFor(DiscreteResource<? super History<$Timeline, ?>, T> resource, Set<T> condition);

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
