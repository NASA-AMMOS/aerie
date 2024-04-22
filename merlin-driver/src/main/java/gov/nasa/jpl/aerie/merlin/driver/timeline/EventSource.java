package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;

public interface EventSource {
  Cursor cursor();

  interface Cursor {
    <State> Cell<State> stepUp(Cell<State> cell);
  }
}
