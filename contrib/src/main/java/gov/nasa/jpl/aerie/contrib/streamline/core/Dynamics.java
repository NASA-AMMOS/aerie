package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/**
 * A single segment of a resource profile;
 * a value which evolves as time passes.
 */
public interface Dynamics<V, D extends Dynamics<V, D>> {
    /**
     * Get the current value.
     */
    V extract();

    /**
     * Evolve for the given time.
     */
    D step(Duration t);
}
