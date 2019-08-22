package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public interface SimulationContext<T extends StateContainer> {

    /**
     * spawns a child activity and blocks until its completion
     */
    public Activity<T> callActivity(Activity<T> childActivity);

    /**
     * spawns a child activity in the background
     */
    public Activity<T> spawnActivity(Activity<T> childActivity);

    /**
     * delays the simulation for some specified amount of time
     */
    public void delay(Duration duration);

    /**
     * blocks until the specified child activity is complete
     */
    public void waitForChild(Activity<T> childActivity);

    /**
     * blocks until all of an activity's children are complete
     */
    public void waitForAllChildren();

    /**
     * delays the effect model until the specified point in time
     */
    public void delayUntil(Time time);

    /**
     * Returns the engine's current simulation time
     * 
     * @return current simulation time
     */
    public Time now();

}
