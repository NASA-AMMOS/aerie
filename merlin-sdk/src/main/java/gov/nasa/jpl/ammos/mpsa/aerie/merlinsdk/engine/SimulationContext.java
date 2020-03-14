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
    SpawnedActivityHandle defer(Duration duration, Activity<?> activity);

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
}
