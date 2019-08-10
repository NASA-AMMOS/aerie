package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class SimulationContext {
    
    private final SimulationEngine engine;
    private final ActivityThread activityThread;

    public SimulationContext(SimulationEngine engine, ActivityThread activityThread) {
        this.engine = engine;
        this.activityThread = activityThread;
    }

    public ActivityThread getActivityThread() {
        return this.activityThread;
    }

    public void delay(Duration d) {
        activityThread.setEventTime(activityThread.getEventTime().add(d));
        engine.pendingEventQueue.add(activityThread);
        activityThread.suspend();
    }

    public void spawnActivity(Activity<?> activity) {
        ActivityThread actThread = new ActivityThread(activity, this.now());
        engine.pendingEventQueue.add(actThread);
    }

    public Time now() {
        return engine.getCurrentSimulationTime();
    }

}