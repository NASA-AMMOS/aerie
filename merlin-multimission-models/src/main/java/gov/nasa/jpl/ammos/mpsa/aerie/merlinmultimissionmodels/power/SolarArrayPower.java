package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * prototype model of the power produced by a solar array based on solar distance
 *
 * measured in Watts
 *
 * this is an initial version of a solar array power model that only accounts for the
 * modulation of power output by the solar distance (which drops off by a factor of
 * r^2) and ignores all other effects such as (a) if the arrays are even deployed yet (b)
 * the angle of incidence of sunlight on the panels (c) degredation of the power output
 * over time (d) self-shadowing of the panels by other spacecraft components (e) the
 * tracking mode of the solar array drive (f) temperature effects on photovoltaic
 * efficiency (g) losses in power conversion and transmission (h) solar eclipses by
 * celestial bodies (i) non-solar illumination sources.
 *
 */
public class SolarArrayPower implements RandomAccessState<Double> {

    //this model would benefit from a generic functional definition of a state in terms
    //of another state, which would obviate all the extra boilerplate

    /**
     * create a default solar array power model with fixed demonstration configuration
     *
     * @param solarDistanceState_m source of distance from the sun to spacecraft, stored
     */
    public SolarArrayPower( RandomAccessState<Double> solarDistanceState_m ) {
        if( solarDistanceState_m == null ) { throw new IllegalArgumentException(
                "specified solar distance state is null" ); }

        this.solarDistanceState_m = solarDistanceState_m;

        //rest of fields use their default demonstration initializers
    }

    /**
     * create a solar array power model utilizing provided configuration constants
     *
     * @param solarDistanceState_m source of distance from the sun to spacecraft, stored
     * @param referencePower_W the power produced at the reference solar distance
     * @param referenceSolarDistance_W distance from sun at which reference power measured
     */
    public SolarArrayPower( RandomAccessState<Double> solarDistanceState_m,
            Double referencePower_W, Double referenceSolarDistance_W ) {
        if( solarDistanceState_m == null ) { throw new IllegalArgumentException(
                "specified solar distance state is null" ); }
        if( referencePower_W < 0.0 ) { throw new IllegalArgumentException(
                "specified solar panel reference power is zero or negative" ); }
        if( referenceSolarDistance_W <= 0.0 ) { throw new IllegalArgumentException(
                "specified reference solar distance is nonsensically zero or negative" ); }

        this.solarDistanceState_m = solarDistanceState_m;
        this.referencePower_W = referencePower_W;
        this.referenceSolarDistance_m = referenceSolarDistance_W;
    }

    /**
     * calculates the power output of the solar panels based on solar distance
     *
     * @return the instantaneous power output of the solar panels, measured in Watts
     */
    @Override
    public Double get() {
        assert this.referenceSolarDistance_m > 0.0
                : "reference solar distance is nonsensically zero or negative";

        //fetch the current solar distance in this query context
        final double currentSolarDistance_m = this.solarDistanceState_m.get();
        if( currentSolarDistance_m <= 0.0 ) { throw new IllegalArgumentException(
                    "current solar distance is nonsensically zero or negative" ); }

        //calculate the modulation factor to apply to the reference power based on an
        // 1/r^2 fall off of solar irradiance from its value at the reference distance
        final double currentDistanceRatio =
                currentSolarDistance_m / this.referenceSolarDistance_m;
        final double distanceFactor = 1 / ( currentDistanceRatio * currentDistanceRatio );

        //apply all modulation factors to the reference power rating of the panels
        final double modulatedPower_W = this.referencePower_W * distanceFactor;

        return modulatedPower_W;
    }


    /**
     * calculates the power output of the solar panels based on solar distance at query time
     *
     * @param queryTime the time at which to calculate the solar power
     * @return the instantaneous power output of the solar panels, measured in Watts
     */
    @Override
    public Double get( Time queryTime ) {
        //replicates functionality of get() but uses interrogates input state at the
        //queryTime rather than the shared simulation context time
        assert this.referenceSolarDistance_m > 0.0
                : "reference solar distance is nonsensically zero or negative";

        //fetch the current solar distance in this query context
        final double currentSolarDistance_m = this.solarDistanceState_m.get(queryTime);
        if( currentSolarDistance_m <= 0.0 ) { throw new IllegalArgumentException(
                "current solar distance is nonsensically zero or negative" ); }

        //calculate the modulation factor to apply to the reference power based on an
        // 1/r^2 fall off of solar irradiance from its value at the reference distance
        final double currentDistanceRatio =
                currentSolarDistance_m / this.referenceSolarDistance_m;
        final double distanceFactor = 1 / ( currentDistanceRatio * currentDistanceRatio );

        //apply all modulation factors to the reference power rating of the panels
        final double modulatedPower_W = this.referencePower_W * distanceFactor;

        return modulatedPower_W;
    }


    /**
     * the power production of the solar panels at reference solar distance
     *
     * measured in Watts
     *
     * this value is used in a simple r^2 law to calculate the power production at other
     * solar distances
     *
     * the default value of 1kW is chosen randomly for demonstration purposes
     */
    private Double referencePower_W = 1000.0;

    /**
     * the solar distance at which the reference power is produced
     *
     * measured in meters
     *
     * this value is used in a simple r^2 law to calculate the power production at
     * other solar distances
     *
     * the default value of 1AU is chosen since most reference powers are quoted at the
     * earth's orbital distance
     */
    private Double  referenceSolarDistance_m = 1.496e+11;

    /**
     * state that can provide the current solar distance measurement
     *
     * must provided values measured in meters
     */
    private RandomAccessState<Double> solarDistanceState_m;



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
    public Map<Time, Double> getHistory() {
        return new LinkedHashMap<Time,Double>();
    }

    /**
     * this is a stop-gap reference to the simulation engine required by the current
     * simulation implementation and used to determine the current simulation time or
     * tag history values. eventually that kind of context would be provided by the
     * engine itself in any call to the state model
     */
    private SimulationEngine simEngine;

}
