package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power.RandomAccessState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * a simple state whose value is computed as a linear interpolation
 */
public class LinearInterpolatedState implements RandomAccessState<Double> {

    /**
     * creates linear interpolation state based on two sample points
     *
     * the two sample points must be at sufficiently distinct times
     *
     * @param t0 the time that the state takes on the initial reference value
     * @param y0 the initial reference state value, valid at t0
     * @param t1 the time that the state takes on y1 value, more than 1ms away from t0
     * @param y1 the second state value, valid at t1, in same units as y0
     */
    public LinearInterpolatedState( Time t0, double y0, Time t1, double y1 ) {

        final double deltaTime_s = t1.subtract(t0).totalSeconds();
        if( deltaTime_s > -0.001 && deltaTime_s < 0.001 ) {
            throw new IllegalArgumentException(
                    "temporal distance between samples is zero or too small (less than 1ms)");
        }

        this.referenceTime = t0;
        this.referenceValue = y0;
        this.slope_u_per_s = ( y1 - y0 ) / deltaTime_s;
    }

    /**
     * time at which the state has the reference value, used to calculate further values
     */
    private final Time referenceTime;

    /**
     * the value of the state at the reference time, used to calculate further values
     */
    private final double referenceValue;

    /**
     * the constant slope of the value with respect to time
     *
     * measured in units/second
     */
    private final double slope_u_per_s;

    /**
     * calculates a linearly interpolated sample at the requested query time
     *
     * uses a linear/constant-slope model between the two sample points provided at
     * construction to compute the value of the state at the query time. query times
     * outside the range of the initial sample points are still calculated using the
     * same fixed slope
     *
     * measured in same units as the provided sample points
     *
     * @param queryTime the simulation time stamp at which to query the value
     * @return the linearly interpolated value of the state at the specified query
     *         time based on sample points provided at construction
     */
    @Override
    public Double get( Time queryTime ) {
        //TODO: this is all to work around the fact that negative Duration is not allowed,
        //      but maybe it should be!
        //      double deltaTime_s = queryTime.subtract( referenceTime ).totalSeconds();
        double deltaTime_s = 0.0;
        if( queryTime.greaterThanOrEqualTo(referenceTime) ) {
            deltaTime_s = queryTime.subtract(referenceTime).totalSeconds();
        } else {
            deltaTime_s = -1.0 * referenceTime.subtract(queryTime).totalSeconds();
        }

        final double resultValue = referenceValue + deltaTime_s * slope_u_per_s;
        return resultValue;
    }

    /**
     * calculates a linearly interpolated sample at simulation engine's current time
     *
     * uses the simulation engine provided by previous call to setEngine() to determine
     * the query time and then returns a linearly interpolated value as above for get(t)
     *
     * @return linearly interpoalted value of the state at current simulation time
     *         based on the sample points provided at construction
     */
    @Override
    public Double get() {
        assert simEngine != null : "simulation engine is null";
        final Time queryTime = simEngine.getCurrentSimulationTime();
        return get(queryTime);
    }

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
