package gov.nasa.jpl.aerie.scheduler;

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
  T getValueAtTime(Time t);

}
