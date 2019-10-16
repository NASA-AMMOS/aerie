package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces;

/**
 * A simulation value that can be directly assigned.
 * <p>
 * A settable state may be assigned to a completely new value by an effect model
 * execution. The new value entirely replaces the prior value going forward (until the
 * next set operation). Subsequent get() calls within a single discrete event will return
 * a value that is equal to the new value, but not necessarily the same object instance.
 * <p>
 * The newly assigned value is not immediately visible to get() calls made from concurrent
 * discrete events from different activity instances, but is visible in all downstream
 * aggregate discrete events. The newly assigned value must comport with any other
 * mutations to the state that occur in the same aggregate discrete event. For example,
 * assignments to different non-equal values at the same instant would be unresolvable,
 * whereas assignments to the same value are allowable.
 * <p>
 * The SettableState interface only express that an effect model may set the state during
 * the simulation: other interfaces describe how the state may be assigned an initial
 * values from known initial conditions.
 *
 * @param <T> the datatype of the measurable value of the state and the newly assigned
 *            values, which must be treated as immutable
 */
public interface SettableState<T> extends State<T> {

    /**
     * Assigns a new value to the state, overwriting any previous value.
     * <p>
     * Must be called within the discrete event execution context of an effect model.
     *
     * The provided new value may be managed by the simulation and must not be
     * modified in any externally visible way after being passed to this method.
     *
     * The newly assigned value is observable in subsequent get() calls made to the
     * state from within the same discrete event execution of an activity instance. A
     * sequence of set operations within such a discrete event is not generally
     * externally visible; only the final set() matters. The new value is not visible
     * to concurrent discrete events from other activity instances, but becomes visible
     * in the next aggregate discrete event.
     *
     * When merging effects from several concurrent discrete events into an aggregate
     * discrete event, only assignments to equal values are consistent. A mix of
     * different non-assignment effects is only consistent with an assignment if the
     * result is unambiguous with respect to order of effects (eg a net
     * increment of 0 and an assignment could be consistent). Any other combination of
     * effects with a set() are inconsistent and will induce a reportable error.
     * <p>
     * This call may record a new dependency node between the instigating activity
     * instance and the downstream state value. These dependencies may be leveraged to
     * efficiently recompute state values and effect models.
     * <p>
     *
     * @param value the new value that the state should take on
     */
    void set(T value);


}
