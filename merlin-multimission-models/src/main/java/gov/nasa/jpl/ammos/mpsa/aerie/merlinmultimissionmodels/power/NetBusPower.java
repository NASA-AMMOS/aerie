package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * prototype model of the net of power generation and loads on the power system
 *
 * measured in Watts
 *
 * the net power is calculated from the various instrument/subsystem loads subtracted
 * from the combined power generations. if the net power is positive, the battery will
 * be gradually recharged, whereas if the net power is negative, the battery will be
 * gradually depleted.
 *
 * this model is currently implemented as a simple linear combination of the
 * specified component loads.
 */
public class NetBusPower implements RandomAccessState<Double>{

    //TODO: this model would benefit from a linear combination state that would
    //      obviate all of the extra boilerplate

    /**
     * create a new net power timeline that is the sum of the provided sources/sinks
     *
     * @param sources_W the power source states to aggregate, which must be measured in
     *                  positive Watts for power generated
     * @param sinks_W the power sink states to aggregate, which must be measured in
     *                positive Watts for power used
     */
    public NetBusPower( List<RandomAccessState<Double>> sources_W,
                        List<RandomAccessState<Double>> sinks_W ) {
        if( sources_W == null ) { throw new IllegalArgumentException(
                "specified power source list is null" ); }
        if( sinks_W == null ) { throw new IllegalArgumentException(
                "specified power sink list is null" ); }
        Consumer<RandomAccessState<Double>> inputCheck = in -> {
            if (in == null) { throw new IllegalArgumentException(
                    "specified power source/sink state is null"); }
            //TODO: would be good to ensure right dimensionality too
        };
        sources_W.forEach( inputCheck );
        sinks_W.forEach( inputCheck );

        this.sources_W = sources_W;
        this.sinks_W = sinks_W;
    }

    /**
     * calculates the net power of all sources minus all sinks
     *
     * measured in Watts
     *
     * positive values reflect net power generation (battery recharge), negative values
     * reflect net power draw (battery drain)
     *
     * @return the net power of all specified sources minus all sinks, in Watts
     */
    @Override
    public Double get() {
        assert this.sources_W != null : "power source list is null";
        assert this.sinks_W != null : "power sink list is null";

        //ignoring caching / dependency graph checking for now (eg no need to recalc if
        //no inputs changed)

        //ignoring order of operations issues with floating point precision for now,
        //eg ( 1E30 + 1E-30 ) - ( 1E30 ) != ( 1E30 - 1E30 ) + 1E-30

        double sourceTotal_W = sources_W.stream().mapToDouble(State::get).sum();
        double sinkTotal_W = sinks_W.stream().mapToDouble(State::get).sum();
        double net_W = sourceTotal_W - sinkTotal_W;

        return net_W;
    }

    /**
     * calculates the net power of all sources minus all sinks at query time
     *
     * measured in Watts
     *
     * positive values reflect net power generation (battery recharge), negative values
     * reflect net power draw (battery drain)
     *
     * @param queryTime the time at which to calculate the net power
     * @return the net power of all specified sources minus all sinks, in Watts
     */
    @Override
    public Double get( Instant queryTime ) {
        //mostly duplicates functionality of get() except that it makes explicit time
        //calls to the sub-states instead of relying on shared sim context (requires
        //the inputs to random access states too!)
        assert this.sources_W != null : "power source list is null";
        assert this.sinks_W != null : "power sink list is null";

        double sourceTotal_W = sources_W.stream().mapToDouble(s->s.get(queryTime)).sum();
        double sinkTotal_W = sinks_W.stream().mapToDouble(s->s.get(queryTime)).sum();
        double net_W = sourceTotal_W - sinkTotal_W;

        return net_W;

    }


    /**
     * the set of all power sources accumulated in the net power calculation
     *
     * measure in Watts
     *
     * positive values reflect power generated (they are added to net power)
     */
    private List<RandomAccessState<Double>> sources_W = new ArrayList<>();

    /**
     * the set of all power sinks accumulated in the net power calculation
     *
     * measured in Watts
     *
     * positive values reflect power consumed (they are subtracted from net power)
     */
    private List<RandomAccessState<Double>> sinks_W = new ArrayList<>();


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
