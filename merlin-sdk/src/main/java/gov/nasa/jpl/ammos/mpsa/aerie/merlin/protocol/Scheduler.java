package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface Scheduler<$Timeline, $ActivityId, $Event, $Activity> {
  void emit($Event event);
  $ActivityId spawn($Activity activity);
  $ActivityId defer(Duration delay, $Activity activity);

  History<$Timeline, $Event> now();
}
