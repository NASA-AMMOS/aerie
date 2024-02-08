package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public interface TaskHandle {
  Scheduler delay(Duration delay);

  Scheduler call(TaskFactory<?> child);

  Scheduler tailCall(TaskFactory<?> child);

  Scheduler await(gov.nasa.jpl.aerie.merlin.protocol.model.Condition condition);
}
