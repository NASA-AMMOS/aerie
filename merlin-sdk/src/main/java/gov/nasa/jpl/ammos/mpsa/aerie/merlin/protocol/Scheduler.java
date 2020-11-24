package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface Scheduler<$Timeline> {
  History<$Timeline> now();
  <Solution> Solution ask(SolvableDynamics<Solution, ?> resource, Duration offset);

  <Event> void emit(Event event, Query<? super $Timeline, Event, ?> query);
  <Spec> String spawn(Spec spec, TaskSpecType<? super $Timeline, Spec> type);
  <Spec> String defer(Duration delay, Spec spec, TaskSpecType<? super $Timeline, Spec> type);
}
