package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/**
 * Interface to a generic state managed by another entity
 *
 * @param <T> the type managed by the state
 */
public interface QueriableState<T> {
  /**
   * Returns the value of the state at time t
   *
   * @param t time
   * @return value
   */
  T getValueAtTime(Duration t);

}
