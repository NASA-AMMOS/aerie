package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.Map;

/**
 * A snapshot of a measurable feature of the simulated system at a specific time.
 * <p>
 * A state is any value that the adaptation cares to track via the simulation. Shared
 * states are one of the primary ways in which activity effect models interact with each
 * other.
 * <p>
 * The State class is the most basal interface provided to the activity effect models so
 * that they can sense their simulated environment via basic get() calls to retrieve the
 * then-current value of the state. Additional state interfaces expand on this vocabulary
 * to allow effect models to pose more complex (eg historical) queries as well as various
 * mutations to the state value.
 * <p>
 * Each read/write to a state from an effect model introduces a possible dependency
 * between the state value at that time and the instigating activity instance. These
 * dependencies may be tracked and evaluated to allow efficient recalculation of state
 * values and effect models.
 * <p>
 * All operations occur from the perspective of the current simulation time point at which
 * an instantaneous effect model is executing. Any concurrent modifications by other
 * effect models are not immediately visible;  all such mutations are merged into a
 * consistent new state value before advancing to the next discrete event. The history of
 * different values of the State over time throughout a simulation are recorded and
 * accessed by a distinct StateHistory interface.
 * <p>
 * Instances of State objects should be documented with javadoc blocks that include any
 * relevant tagging used to describe the state semantics, its units, its logical grouping,
 * etc. These attached documentation blocks may be used to integrate with other systems
 * such as flight rules, constraint checkers, state dictionaries, etc.
 *
 * @param <T> the datatype of the measurable value of the state, which must be treated as
 *            immutable
 */
public interface State<T> {

    /**
     * Fetch the value of the state at the current simulation time point
     * <p>
     * Must be called within the discrete event execution context of an effect model.
     * <p>
     * The value may be computed dynamically according to relevant interpolation or
     * derivation functions. The returned value object is considered immutable and may not
     * be used to effect changes on the state; instead use the methods exposed on the
     * State's interface itself.
     * <p>
     * The observed value reflects the state as of the beginning of the current simulation
     * discrete event plus any modifications made by the current active effect model. Thus
     * subsequent get()s may yield different results even within a discrete event.
     * <p>
     * The observed value does NOT include any concurrent modifications made by other
     * effect models in the same simulation aggregate discrete event, only modifications
     * from prior discrete events. Note that two discrete events may occur within the same
     * temporal moment, eg by one effect model waiting on a condition posted by another.
     * <p>
     * This call may record a dependency between the calling activity effect model
     * execution and the value of the state at this time. These dependencies may be
     * leveraged to efficiently recompute state values and effect models.
     * <p>
     *
     * @return the value of the state at the current simulation time point
     */
    T get();


    /**
     * All states should have a UID, which is currently its name
     * This will be used by other states to name dependencies
     * This String can serve as a key to any values we store and later retrieve regarding this state
     * @return the name of the state
     */
    public String getName();


    //TODO: Refactor sim engine to no longer require this at this state level
    /**
     * This is a temporary method used to enable the current SimulationEngine unit tests
     * to compile and run without modification (yet). It is used to time-tag state
     * assignments so that the history can be reported to the unit tests for the
     * simulation engine.
     *
     * @param engine the controlling simulation engine
     */
    public void setEngine(SimulationEngine<?> engine);

    //TODO: Refactor sim engine to no longer require this at this state level
    /**
     * this is a temporary method in order to integrate w/ the current SimulationEngine
     * unit tests
     * <p>
     * the unit tests currently need a way to inspect the state history directly
     */
    public Map<Time, T> getHistory();
}

