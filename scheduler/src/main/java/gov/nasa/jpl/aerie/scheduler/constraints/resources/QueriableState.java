package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

/**
 * Interface to a generic state managed by another entity
 *
 */
public interface QueriableState {
  /**
   * Returns the value of the state at time t
   *
   * @param t time
   * @return value
   */
  SerializedValue getValueAtTime(Duration t);

}
