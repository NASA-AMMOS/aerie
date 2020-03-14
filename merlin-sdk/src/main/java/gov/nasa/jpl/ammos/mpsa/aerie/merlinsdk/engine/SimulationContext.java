package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

public interface SimulationContext {
    /**
     * spawns a child activity in the background
     */
    SpawnedActivityHandle spawnActivity(Activity<?> childActivity);

    /**
     * delays the simulation for some specified amount of time
     */
    void delay(Duration duration);

    /**
     * blocks until all of an activity's children are complete
     */
    void waitForAllChildren();

    /**
     * Returns the engine's current simulation time
     * 
     * @return current simulation time
     */
    Instant now();


    interface SpawnedActivityHandle {
        void await();
    }


    /**
     * Spawns a child activity and blocks on the completion of its effect model
     *
     * If non-blocking behavior is desired, see `spawnActivity()`.
     *
     * @param childActivity the child activity that should be spawned and blocked on
     */
    default void callActivity(final Activity<?> childActivity) {
        this.spawnActivity(childActivity).await();
    }

    default void delayUntil(Instant time) {
        this.delay(time.durationFrom(this.now()));
    }

    default void delay(final long quantity, final TimeUnit units) {
        this.delay(Duration.fromQuantity(quantity, units));
    }
}
