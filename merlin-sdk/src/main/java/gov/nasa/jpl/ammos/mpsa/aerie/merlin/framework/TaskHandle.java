package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;

public interface TaskHandle<$Timeline> {
  Scheduler<$Timeline> yield(TaskStatus<$Timeline> status);
}
