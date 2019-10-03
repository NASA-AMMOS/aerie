package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;

/**
 * This class is the most basal interface provided to the activity effect models
 * so that they can sense their simulated environment. A State object contains a
 * snapshot of a measurable feature of the simulated system. A State can
 * represent any value that the adaptation cares to track via simulation.
 *
 * Activity effect models can get and set the values of states. During
 * simulation, these effect models are executed and a State's value can be
 * changed. As multiple activities get and set a State's value, dependencies
 * between the State and these activities are created.
 *
 * A State object reflects the value of the State at a specific point in time.
 * Another object, the State History, will record the different values of the
 * State over time. This State History can be populated during simulation.
 *
 * @param <T> the datatype of the measurable value of the state
 *
 */

public interface State<T> {

    /**
     * fetch the value of the state at the current simulation time slice
     *
     * must be called within the execution context of an effect model
     * note that the returned value is as of the beginning of the time slice at
     * which the calling activity effect model is executing, and therefore does
     * not reflect any modifications queued up by any activities executing at
     * the same instant (including the calling one!). it is thus constant
     * throughout the time slice.
     *
     * the value returned may be computed dynamically according to the relevant
     * state's simulation history and designated interpolation/derivation
     * functions. as such, the returned value object cannot be used to directly
     * change the state's underlying simulation value.
     *
     * future plans: this call records within the simulation that the calling activity effect
     * model interrogated this state, creating an corresponding entry in the
     * dynamic dependency graph (if enabled).
     *
     * @return the value of the state at the current simulation time
     */
    public T getValue();

    /**
     * calling of this method is similar to getValue (must be called within execution
     * context of an effect model).
     *
     * the activity, via the effect model, can change the value of the state.  This should also be recorded
     * within the simulation (again, similar to getValue()).
     *
     * @param value
     */
    public void setValue(T value);

    /**
     * Returns the string name of the state, supplied by the adapter
     *
     * @return String name
     */
    public String getName();

    /**
     * Registers the given simulation engine as the controlling engine for this state
     * 
     * @param engine the controlling simulation engine
     */
    public void setEngine(SimulationEngine<?> engine);

}