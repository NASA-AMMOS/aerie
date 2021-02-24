package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.merlin.timeline.Query;

public interface Checkpoint<$Timeline> {
  <Event, Model> Model ask(final Query<? super $Timeline, Event, Model> query);
}
