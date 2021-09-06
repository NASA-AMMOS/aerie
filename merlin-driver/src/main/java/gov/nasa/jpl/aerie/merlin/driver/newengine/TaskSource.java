package gov.nasa.jpl.aerie.merlin.driver.newengine;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;

/** The source of the definition of a task's behavior. */
public interface TaskSource<$Timeline> {
  Task<$Timeline> createTask();
}
