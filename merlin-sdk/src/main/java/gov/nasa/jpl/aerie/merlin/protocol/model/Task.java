package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

public interface Task<Return> extends AutoCloseable {
  /**
   * Perform one step of the task, returning the conditions under which to progress to the next step.
   *
   * If this method returns {@link TaskStatus.Completed}, then any system resources held must be released.
   * See {@link #reset()} for more details on resource management.
   */
  TaskStatus<Return> step(Scheduler scheduler);

  /**
   * Reset this task to its state before {@link #step(Scheduler)} was ever called.
   *
   * Any system resources held must be released by this method, so that garbage collection can take care of the rest.
   * For instance, if this object makes use of an OS-level Thread, that thread must be explicitly released to avoid
   * resource leaks.
   */
  void reset();

  @Override
  default void close() {
    this.reset();
  }
}
