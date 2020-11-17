package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface Scheduler<$Timeline, TaskSpec> {
  History<$Timeline> now();
  <Solution> Solution ask(SolvableDynamics<Solution, ?> resource, Duration offset);

  <Event> void emit(Event event, Query<? super $Timeline, Event, ?> query);
  String spawn(TaskSpec taskSpec);
  String defer(Duration delay, TaskSpec taskSpec);
}
