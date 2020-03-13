package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.Map;
import java.util.TreeMap;

/**
 * prototype model of power consumed by an instrument
 *
 * measured in Watts
 *
 * this is an initial version of an instrument power draw model that is assigned
 * directly rather than being eg computed from a combination of other control states
 * or operation modes of the instrument (eg on, off, survival heater, low-power,
 * high-power, etc). as such, this state's power draw is understood to encompass all of
 * the instrument subsystem's various loads, whether controlled by the instrument or
 * the spacecraft (eg the survival heaters). furthermore, no accounting of recycled heat
 * is made to offset this draw with some reclaimed thermal load. in a more advanced
 * implementation, each of these detailed aspects would be a separate component state
 * that would be rolled up in an overall sum for the instrument subsystem.
 *
 * note that this implementation is only designed to work with the current simple
 * forward simulation architecture; it will need improvements to support non-synchronous
 * execution of models.
 */
public class InstrumentPower implements SettableState<Double>, RandomAccessState<Double> {

    /**
     * initialize an instrument power model with default demonstration configuration
     *
     * power states start in an "off" 0.0W state
     */
    public InstrumentPower() {
        power_W = 0.0;
    }

    /**
     * calculates the power draw of the instrument
     *
     * currently just based on the last assigned value to this state (not on mode etc)
     *
     * @return the instantaneous power draw of the instrument, measured in Watts
     */
    @Override
    public Double get() {
        return power_W;
    }

    /**
     * determines the power draw of the instrument at some past query time
     *
     * does not work for future time queries since the power draw is commanded
     * by the instrument activities during simulation
     *
     * @param queryTime the past time point to fetch instrument power draw of
     * @return the instantaneous power draw of the instrument at the queryTime,
     *         measured in Watts
     */
    @Override
    public Double get( Instant queryTime ) {
        assert ! powerHistory_W.isEmpty() : "history is empty";
        if( queryTime.isAfter( SimulationEffects.now() ) ) {
            throw new IllegalArgumentException( "query for state value in future" ); }

        //note that history initializer ensures that there is always a lowest key,
        //even if no set() has been called yet
        //
        //using floorEntry (<=) semantics instead of lowerEntry (<) so that
        //activities can observe their own contemporaneous effects
        return powerHistory_W.floorEntry( queryTime ).getValue();
    }

    /**
     * directly assigns a new total instrument power draw
     *
     * the new power draw takes effect immediately and persists until changed again
     *
     * note that in future implementations, this total power draw will instead be
     * calculated based on instrument operation mode, temperature, etc
     *
     * @param value_W the new power draw of the instrument, measured in Watts
     */
    @Override
    public void set( Double value_W ) {
        power_W = value_W;

        final Instant setTime = SimulationEffects.now();
        assert !setTime.isBefore( powerHistory_W.lastKey() )
                : "set occurs at simulation time prior to last set call";
        powerHistory_W.put( setTime, value_W ); //discard prior value if any
    }


    /**
     * single stored current value of the instrument power draw
     *
     * this is limited to working in forward synchronous simulations only!
     */
    private double power_W = 0.0;

    /**
     * history of all past assignments to the instrument state
     *
     * used to provide the get(t) method for random access state interface
     *
     * TODO: could obviate power_W since last key will be equivalent, but keeping
     *       separate for now until we figure out our approach to state cross-queries
     */
    private TreeMap< Instant, Double > powerHistory_W = new TreeMap<>();

    //----- temporary members/methods to appease old simulation engine -----

    @Override
    public String getName() {
        //TODO: requires returning a unique identifier for the state, but I don't think
        //      the state itself should own such identifiers (eg it can't ensure global
        //      uniqueness, prevents one state having synonyms used in different contexts,
        //      and is liable to become brittle hard-coded references that won't support
        //      swapping out states). The naming should probably be left to an overall
        //      state registry functionality that organizes/fetches relevant states.
        //      in the meantime, uniqueness is served fine by the java object id
        return super.toString();
    }

    @Override
    public void initialize(final Instant initialTime) {
        // Insert the initial power value as soon as we know the earliest simulation time.
        powerHistory_W.put(initialTime, power_W);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Instant, Double> getHistory() {
        return powerHistory_W;
    }

}
