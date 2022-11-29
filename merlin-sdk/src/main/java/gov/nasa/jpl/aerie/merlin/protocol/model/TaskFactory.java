package gov.nasa.jpl.aerie.merlin.protocol.model;

import java.util.concurrent.Executor;

/**
 * A factory for creating fresh copies of a task. All tasks created by a factory must be observationally equivalent.
 *
 * @param <Input>
 *   The type of data required by a task created by this factory.
 * @param <Output>
 *   The type of data returned by a task created by this factory.
 */
public interface TaskFactory<Input, Output> {
  Task<Input, Output> create(Executor executor);
}
