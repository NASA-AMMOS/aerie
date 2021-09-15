package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.Capability;

/**
 * Designates the simulation state at a particular time in the simulation timeline.
 *
 * @param <$Timeline> The simulation timeline indexed by this checkpoint.
 */
@Capability
public interface Checkpoint<$Timeline> {}
