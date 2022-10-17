package gov.nasa.jpl.aerie.merlin.protocol.model;

import java.util.concurrent.ExecutorService;

/**
 * A factory for creating fresh copies of a task. All tasks created by a factory must be observationally equivalent.
 *
 * @param <Return>
 *   The type of data returned by a task created by this factory.
 */
public interface TaskFactory<Return> {
  Task<Return> create(ExecutorService executor);
}
