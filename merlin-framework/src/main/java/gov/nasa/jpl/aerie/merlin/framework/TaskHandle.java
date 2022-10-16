package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public interface TaskHandle {
  Scheduler delay(Duration delay);

  Scheduler call(Task<?> child);

  Scheduler await(gov.nasa.jpl.aerie.merlin.protocol.model.Condition condition);
}
