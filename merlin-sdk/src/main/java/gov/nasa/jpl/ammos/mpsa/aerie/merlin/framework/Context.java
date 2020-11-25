package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Map;
import java.util.Set;

public interface Context<$Schema> {
  History<? extends $Schema> now();
  double ask(RealResource<? super History<? extends $Schema>> resource);
  <T> T ask(DiscreteResource<? super History<? extends $Schema>, T> resource);

  <Event> void emit(Event event, Query<? super $Schema, Event, ?> query);
  <Spec> String spawn(Spec spec, TaskSpecType<? super $Schema, Spec> type);
  <Spec> String defer(Duration duration, Spec spec, TaskSpecType<? super $Schema, Spec> type);

  void delay(Duration duration);
  void waitFor(String id);
  void waitFor(RealResource<? super History<? extends $Schema>> resource, RealCondition condition);
  <T> void waitFor(DiscreteResource<? super History<? extends $Schema>, T> resource, Set<T> condition);
}
