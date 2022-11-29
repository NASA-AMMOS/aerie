package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;

/** The source of the definition of a task's behavior. */
public interface TaskSource<Input, Output> {
  Task<Input, Output> createTask();
}
