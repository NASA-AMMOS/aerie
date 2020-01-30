package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

/**
 * allows access to a state value at any query time (not just current sim time)
 *
 * this is a stop-gap interface bastardization in order to accomplish the 29.0 delivery;
 * we intend to clean up our state interface scheme to handle this in future deliveries
 *
 * typically this is only possible for states that don't depend on any activities or
 * other (non-random access) states that are synchronized by the simulation itself. for
 * example, the SolarDistance would be computable directly from emphemeris data, and so
 * does not require a live simulation
 *
 *  @param <T> the datatype of the measurable value of the state, which must be treated
 *             as immutable
 */
public interface RandomAccessState<T> extends State<T> {

    /**
     * fetch the value of the state at the queried time point
     *
     * may be called outside of any discrete event execution context of an effect model,
     * or even outside of a simulation run at all
     *
     * @param queryTime the simulation time stamp at which to query the value
     *
     * @return the value of the state at the queried time point
     */
    T get( Instant queryTime );

}
