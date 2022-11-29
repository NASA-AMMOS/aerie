package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

public interface Task<Input, Output> {
  /**
   * Perform one step of the task, returning the next step of the task and the conditions under which to perform it.
   *
   * <p>Clients must only call {@code step()} at most once, and must not invoke {@code step()} after {@link #release()}
   * has been invoked.</p>
   */
  TaskStatus<Output> step(Scheduler scheduler, Input input);

  /**
   * Release any transient system resources allocated to this task.
   *
   * <p>Any system resources held must be released by this method, so that garbage collection can take care of the rest.
   * For instance, if this object makes use of an OS-level Thread, that thread must be explicitly released to avoid
   * resource leaks</p>
   *
   * <p>This method <b>shall not</b> be called on this object after invoking {@code #step(Scheduler)};
   * nor shall {@link #step(Scheduler, Input)} be called after this method.</p>
   */
  default void release() {}
}
