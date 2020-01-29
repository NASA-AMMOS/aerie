package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

public interface SimulationContext {

    /**
     * spawns a child activity and blocks until its completion
     */
    public Activity<?> callActivity(Activity<?> childActivity);

    /**
     * spawns a child activity in the background
     */
    public Activity<?> spawnActivity(Activity<?> childActivity);

    /**
     * delays the simulation for some specified amount of time
     */
    public void delay(Duration duration);

    /**
     * blocks until the specified child activity is complete
     */
    public void waitForChild(Activity<?> childActivity);

    /**
     * blocks until all of an activity's children are complete
     */
    public void waitForAllChildren();

    /**
     * delays the effect model until the specified point in time
     */
    public void delayUntil(Instant time);

    /**
     * Returns the engine's current simulation time
     * 
     * @return current simulation time
     */
    public Instant now();


    default void delay(long quantity, TimeUnit units) {
        this.delay(Duration.fromQuantity(quantity, units));
    }
}
