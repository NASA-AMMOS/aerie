package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * implements a simple model of an instrument powering on
 *
 * the power load from the instrument is set directly on a designated instrument power
 * state, which is for now specified by java reference
 * TODO: the instrument state specification should probably be a registered name once
 *       we get that kind of cross-referential functionality in our state containers
 *
 * more complex models may abstract this assignment into eg a mode that is set, that
 * then results in a calculation of the instrument power given other environmental factors.
 *
 * @param <StatesT> the state container class provided by the simulation engine to activities
 */
public class InstrumentOn<StatesT extends StateContainer> implements Activity<StatesT> {

    /**
     * the constant power load that the instrument will consume after it is turned on
     * by this activity up until it is switched off by a later InstrumentOff activity
     *
     * measured in Watts
     */
    @Parameter
    public double powerLoad_W;

    /**
     * reference to the instrument power state that should be modified when the
     * instrument turns on during this activity
     *
     * should be a member of the StatesT structure
     *
     * measured in Watts
     *
     * TODO: this should be some kind of cross-reference to a state, but don't have
     *       that kind of functionality in our state containers as yet
     */
    @Parameter
    public InstrumentPower instrumentPowerState_W;

    /**
     * default ctor required by activity interface; parameters populated via injection
     */
    public InstrumentOn() { }

    /**
     * creates a new power-on activity that will set the powerState to provided load
     *
     * @param powerLoad_W the constant power load that instrument draws while it is on,
     *                    measured in Watts
     * @param powerState_W the power state that should be updated by this activity, which
     *                   should be a member of the StatesT, measured in Watts
     */
    public InstrumentOn( double powerLoad_W, InstrumentPower powerState_W ) {
        this.powerLoad_W = powerLoad_W;
        this.instrumentPowerState_W = powerState_W;
    }

    /**
     * confirms that all constructed or injected parameter values are consistent:
     * - the specified instrument power state reference is valid
     * - the power load is non-negative
     *
     * @return list of validation failures, or an empty list if no failures occurred.
     */
    public List<String> validateParameters() {
        List<String> errors = new ArrayList<>();

        if( this.powerLoad_W < 0.0 ) {
            errors.add( "specified instrument power load is negative" );
        }

        //TODO: once we have cross-references, should validate cross-reference exists
        //      but would need a StatesT to do that...
        if( this.instrumentPowerState_W == null ) {
            errors.add( "specified instrument power state is null or not found" );
        }

        //TODO: would be nice to check the dimensionality/units of params too

        return errors;
    }

    /**
     * executes the simulation effects of the activity within the given context:
     * - sets the target instrument power load to designated value
     *
     */
    @Override
    public void modelEffects( StatesT states ) {
        assert( validateParameters().isEmpty() );

        //find the specified instrument's power state
        //TODO: should pull up actual state to modify from the states context provided,
        //      but currently no way to navigate such cross-references in merlin
        InstrumentPower instrumentPowerState_W = this.instrumentPowerState_W;

        //immediately turn on instrument
        instrumentPowerState_W.set( this.powerLoad_W );
    }


}
