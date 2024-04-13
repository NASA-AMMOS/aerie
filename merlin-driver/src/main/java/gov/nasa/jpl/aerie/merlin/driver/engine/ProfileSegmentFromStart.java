package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/**
 * A period of time over which a dynamics occurs.
 * @param startOffset The duration from the start of the plan to the start of this segment
 * @param dynamics The behavior of the resource during this segment
 * @param <Dynamics> A choice between Real and SerializedValue
 */
public record ProfileSegmentFromStart<Dynamics>(Duration startOffset, Dynamics dynamics) {
}
