package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public interface Resource<Dynamics> {
  String getType();

  OutputType<Dynamics> getOutputType();

  /**
   * Get the current value of this resource.
   *
   * <p> The result of this method must vary only dependent on the cells allocated by the model instance that registered
   * this resource. In other words, it cannot depend on any hidden state. </p>
   */
  Dynamics getDynamics(Querier querier);

  /**
   * After a simulation completes the entire evolution of the dynamics of this resource will typically be serialized as
   * a resource profile consisting of some number of sequential segments.
   *
   * If run length compression is allowed for this resource then whenever there is a "run" of two or more such segments,
   * one after another with the same dynamics, they will be compressed into a single segment during that serialization.
   * This does not change the represented evolution of the dynamics of the resource, but it loses the information that a
   * sample was taken at the start of each segment after the first in such a run.  If a mission model prefers not to
   * lose that information then it can return false here.
   */
  default boolean allowRunLengthCompression() {
    return false;
  }
}
