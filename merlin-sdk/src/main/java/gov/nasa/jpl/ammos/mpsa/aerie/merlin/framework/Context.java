package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Set;

public interface Context<$Schema, TaskSpec> {
  History<? extends $Schema> now();
  double ask(RealResource<? super History<? extends $Schema>> resource);
  <T> T ask(DiscreteResource<? super History<? extends $Schema>, T> resource);

  <Event> void emit(Event event, Query<? super $Schema, Event, ?> query);
  String spawn(TaskSpec taskSpec);
  String defer(Duration duration, TaskSpec taskSpec);

  void delay(Duration duration);
  void waitFor(String id);
  void waitFor(RealResource<? super History<? extends $Schema>> resource, RealCondition condition);
  <T> void waitFor(DiscreteResource<? super History<? extends $Schema>, T> resource, Set<T> condition);
}
