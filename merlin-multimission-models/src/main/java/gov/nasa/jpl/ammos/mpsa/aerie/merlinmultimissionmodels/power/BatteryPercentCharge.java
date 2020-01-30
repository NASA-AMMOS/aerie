package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * prototype model of battery state of charge (in percentage terms)
 *
 * measured in (unitless ratio x100) percentage of current battery capacity
 *
 * this state calculates the battery state of charge based on the battery stored
 * energy divided by the battery capacity (x100 to get a percentage). note that many
 * flight rules are framed in terms of these battery percentages
 */
public class BatteryPercentCharge implements State<Double> {

    //TODO: this state would benefit from a simple equational relationship state that
    //      would obviate all the extra boilerplate

    /**
     * creates a new state of charge state calculated from provided energy measures
     *
     * @param storedState_J a state that reports the energy stored in the battery,
     *                      measured in Joules
     * @param capacityState_J a state that reports the maximum capacity that the battery
     *                        could possibly store, measured in Joules
     */
    public BatteryPercentCharge( State<Double> storedState_J, State<Double> capacityState_J ) {
        if( storedState_J == null ) { throw new IllegalArgumentException(
                "specified stored energy state is null" ); }
        if( capacityState_J == null ) { throw new IllegalArgumentException(
                "specified energy capacity state is null" ); }
        //TODO: would be good to ensure right dimensionality/units too

        this.storedState_J = storedState_J;
        this.capacityState_J = capacityState_J;
    }

    /**
     * calculates the percentage charge of the battery based on stored energy and capacity
     *
     * measured in unitless x100 percentage of capacity
     *
     * @return the state of charge of the battery, in unitless x100 percent
     */
    @Override
    public Double get() {
        assert this.capacityState_J != null : "battery capacity state is null";
        assert this.storedState_J != null : "battery stored energy state is null";

        //determine the stored energy and capacity in the current query context
        double capacity_J = this.capacityState_J.get();
        double stored_J = this.storedState_J.get();

        //calculate percentage charge
        if( capacity_J <= 0.0 ) { throw new IllegalArgumentException(
                "battery capacity is nonsensically zero or negative"); }
        double charge_pct = 100.0 * stored_J / capacity_J;

        return charge_pct;
    }

    /**
     * state that calculates the energy stored within the battery after accounting for
     * various power loads and power generation
     *
     * measured in Joules
     */
    private State<Double> storedState_J;

    /**
     * state the calculates the maximum energy that can be stored by the battery after
     * accounting for various capacity fade from charging cycles and other deterioration
     *
     * measured in Joules
     */
    private State<Double> capacityState_J;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEngine(SimulationEngine engine) {
        this.simEngine = engine;
    }

    /**
     * {@inheritDoc}
     *
     * since this is a stopgap, this method is non-functional: just returns an empty map
     */
    @Override
    public Map<Instant, Double> getHistory() {
        return new LinkedHashMap<Instant,Double>();
    }

    /**
     * this is a stop-gap reference to the simulation engine required by the current
     * simulation implementation and used to determine the current simulation time or
     * tag history values. eventually that kind of context would be provided by the
     * engine itself in any call to the state model
     */
    private SimulationEngine simEngine;
}
