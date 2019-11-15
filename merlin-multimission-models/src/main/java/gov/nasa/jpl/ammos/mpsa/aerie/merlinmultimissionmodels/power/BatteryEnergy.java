package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.events.EventHandler;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.nonstiff.RungeKuttaIntegrator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * prototype model of the stored battery energy as it discharges/recharges
 *
 * measured in Joules
 *
 * the accumulated battery energy is calculated as the time integral of the provided
 * net power throughput after all sources and sinks have been accounted, starting at
 * some initial stored energy value. while the net power is positive, the battery will
 * gradually recharge, and while the net power is negative, the battery will gradually
 * discharge.
 *
 * the battery charging saturates at the provided upper limit over which extra power
 * input is shunted away by the battery electronics to prevent an overcharging condition
 *
 * this battery model does *not* saturate at a lower limit (such as 0.0J) since this
 * kind of condition can/should be detected by the planning system constraints and
 * brought to user attention. for example, they may be interested in how much the
 * battery was over-drawn.
 * TODO: is this the semantic that is most useful?
 *
 * this battery stored energy is different from the battery state of charge as a
 * percentage, which is calculated from this stored energy value
 *
 * the current implementation is only designed to operate in a forward simulation
 */
public class BatteryEnergy implements State<Double> {

    //TODO: this model would benefit from a time integral state that would obviate all
    //      of the extra boilerplate

    /**
     * create battery energy state model using provided initial values and input states
     *
     * @param initialCharge_J the initial energy level stored in the battery, measured
     *                        in Joules
     * @param initialTime the time at which the initial charge was measured
     * @param netPower_W state representing the net power input/draw on the battery,
     *                   with positive values recharging the battery and negative values
     *                   discharging it, measured in Watts
     * @param maxCharge_J state representing the maximum (possibly infinite) energy that
     *                    can be stored in the battery, after which any excess recharged
     *                    energy is shunted away, measured in Joules
     */
    public BatteryEnergy( double initialCharge_J, Time initialTime,
                          RandomAccessState<Double> netPower_W,
                          RandomAccessState<Double> maxCharge_J ) {

        if( netPower_W == null ) { throw new IllegalArgumentException(
                "provided net power state is null" ); }
        if( maxCharge_J == null ) { throw new IllegalArgumentException(
                "provided net power state is null" ); }
        if( initialTime == null ) { throw new IllegalArgumentException(
                "provided initial time is null" ); }

        //TODO: would be good to ensure right dimensionality/units too

        this.lastCharge_J = initialCharge_J;
        this.lastCalculationT = initialTime;
        this.netPowerState_W = netPower_W;
        this.maxChargeState_J = maxCharge_J;
    }

    /**
     * calculates the energy stored in the battery in the current query time-slice
     *
     * accounts for power load/generation changes since the last recalculation time
     * TODO: currently assumes a forward simulation!
     *
     * @return the energy that is stored in the battery
     */
    @Override
    public Double get() {
        assert this.netPowerState_W != null : "net power state is null";
        assert this.maxChargeState_J != null : "maximum charge state is null";
        assert this.lastCalculationT != null : "last calculation time is null";
        assert this.simEngine != null : "simulation engine is null";

        //to be fully accurate, we should ask more of the net power state: instead
        //of single values at a query times, it would be better to understand the
        //intervals of time over which integration methods are appropriate and where
        //the discontinuities occur. this implementation is an interim approach until
        //we have access to dependency graph information for state dependencies wherein
        //we could explicitly determine when the critical points in inputs are (eg power
        //loads turn on, solar arrays are steered away from the sun).
        //maybe even better would be to just ask the power state for its own integral
        //expression, which could (conceivably) be composed from its own inputs.

        //TODO: this critically assumes that the simulation is driven forward in time only
        //(though only really to save itself from re-integrating the entire timeline each query)
        Time curT = this.simEngine.getCurrentSimulationTime();
        assert curT.greaterThanOrEqualTo( this.lastCalculationT )
                : "query for value in past, but only implemented for forward simulation";

        //short circuit on already-calculated value
        if( curT.equals( lastCalculationT ) ) {
            return lastCharge_J;
        }

        //calculate the duration of forward integration in seconds
        final Duration integrationDur = curT.subtract( lastCalculationT );
        final double integrationDur_s = integrationDur.totalSeconds();

        //set up integration of the net power state to arrive at net energy delta
        //
        //ref: commons.apache.org/proper/commons-math/userguide/analysis.html
        //ref: commons.apache.org/proper/commons-math/userguide/ode.html
        //TODO: using some magic numbers for integrator configuration for now
        double stepSize_s = 1.0;
        double maxEventStepSize_s = 600.0;
        double eventConvergance_s = 1.0;
        int maxEventIters = 1000000;
        FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator( stepSize_s );

        //the input power state (the derivative of energy) is provided via an ODE interface
        //NB: all integrator-internal times are based off of zero point lastCalculationT
        FirstOrderDifferentialEquations powerODE = new FirstOrderDifferentialEquations(){

            //returns count of entries in the state vector, for us just 1 (energy)
            @Override public int getDimension() {
                //y[0] := battery energy
                return 1;
            }

            //calculates the partial derivatives of the state vector, for us just 1 (power)
            //the query times are determined (possibly dynamically) by the integrator
            //but are guaranteed to be at least at "events" from the event handler, below
            @Override public void computeDerivatives( double t, double[] y, double[] yDot) {
                final double energy_J = y[0];

                //calculate query time based on integration starting at last calc
                //(to avoid conflating integrators/merlin's time representations)
                //TODO: the duration/time api needs some work to avoid exposed representation
                final Time queryTime = lastCalculationT.add( Duration.fromSeconds(t) );

                //query the input states for their values at requested time point
                final double maxCharge_J = maxChargeState_J.get(queryTime);
                final double inputPower_W = netPowerState_W.get(queryTime);

                //normally the derivative of battery energy is just the net power
                double energyDerivative_W = inputPower_W;

                //except it saturates if still charging and at max charge already
                if( ( inputPower_W > 0.0 ) && ( energy_J >= maxCharge_J ) ) {
                    energyDerivative_W = 0.0;
                    //NB: don't need to turn derivative negative to shed excess since
                    //    that clamping occurs in the event handler below
                    //TODO: could reframe to be event-less by modifying derivitive to
                    //      shed any excess charge over maximum
                }

                //copy derivative value to provided output space
                yDot[0] = energyDerivative_W;
            }
        };//powerODE

        //add handling for overcharge saturation conditions via an EventHandler interface
        integrator.addEventHandler( new EventHandler() {
            @Override public void init(double t_0, double[] y, double t_max) {}
            //provides g()==0 clues to integrator about when discontinuities occur;
            //must be continuous near roots and must change sign after each!
            @Override public double g(double t, double[] y) {
                final Time queryTime = lastCalculationT.add( Duration.fromSeconds(t) );

                //an overcharge event occurs when the current charge exceeds max
                final double energy_J = y[0];
                final double maxCharge_J = maxChargeState_J.get(queryTime);
                return ( maxCharge_J - energy_J );
                //TODO: is this enough? seems like could get lots in a row!
            }
            //determines action to take when a g()==0 occurs: for us, clamp to max charge
            @Override public Action eventOccurred(double t, double[] doubles, boolean b) {
                //overcharge immediately resets charge to maximum
                return Action.RESET_STATE;
            }
            //modifies the state vector after a g()==0 event and RESET_STATE occur
            @Override public void resetState(double t, double[] y) {
                final Time queryTime = lastCalculationT.add( Duration.fromSeconds(t) );

                //reset to maximum charge
                final double maxCharge_J = maxChargeState_J.get(queryTime);
                y[0] = maxCharge_J;
            }
        }, maxEventStepSize_s, eventConvergance_s, maxEventIters );

        //run the integration (it writes to the y_end state vector)
        final double t_0 = 0.0; //NB: offset by lastCalculationT in powerODE
        final double[] y_0 = new double[] { lastCharge_J };
        final double t_end = integrationDur_s;
        double[] y_end = new double[] { 0.0 };
        final double t_stop = integrator.integrate( powerODE, t_0, y_0, t_end, y_end );
        assert ( t_stop - t_end ) <= eventConvergance_s : "integration terminated early";
        final double finalCharge_J = y_end[0];

        //update internal estimates (depends on forward simulation assumption!)
        this.lastCharge_J = finalCharge_J;
        this.lastCalculationT = curT;

        return finalCharge_J;
    }

    /**
     * the energy stored within the battery at the last calculation time
     *
     * measured in Joules
     *
     * TODO: this assumes a forward simulation!
     */
    private double lastCharge_J;

    /**
     * the last time at which the battery charge was calculated
     *
     * TODO: this assumes a forward simulation!
     */
    private Time lastCalculationT;

    /**
     * state representing the net power input/draw on the battery, with positive values
     * recharging the battery and negative values discharging it
     *
     * measured in Watts
     */
    private RandomAccessState<Double> netPowerState_W;

    /**
     * state representing the (possibly infinite) maximum energy that can be stored in
     * the battery, after which any excess recharged energy is shunted away. typically
     * this would be the maximum capacity after accounting for battery fade and
     * degredation.
     *
     * measured in Joules
     *
     * a null state indicates there is no such limit
     */
    private RandomAccessState<Double> maxChargeState_J;


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
