package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.function.Consumer;

public interface SimulationContext {
    /**
     * Spawn a new activity as a child of the currently-running activity after a given span of time.
     */
    SpawnedActivityHandle defer(Duration duration, Consumer<SimulationContext> childActivity);

    /**
     * Delay the currently-running activity for the given duration.
     */
    void delay(Duration duration);

    /**
     * Delay the currently-running activity until all of its existing children have completed.
     */
    void waitForAllChildren();

    /**
     * Get the current simulation time.
     */
    Instant now();

    /**
     * Get the container of states registered for the active simulation.
     *
     * DEPRECATED: This exists only as an internal hack to allow Activity#modelEffects to receive a state container
     * as an argument. We may architect things so that an activity has other means of looking up states, so the present
     * solution is tentative at best.
     */
    @Deprecated(forRemoval = true)
    StateContainer getActiveStateContainer();

    interface SpawnedActivityHandle {
        /**
         * Delay the currently-running activity until the activity described by this handle has completed.
         */
        void await();
    }
}
