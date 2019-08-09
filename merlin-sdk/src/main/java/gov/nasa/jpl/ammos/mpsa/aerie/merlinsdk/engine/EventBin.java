package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.HashSet;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class EventBin extends HashSet<ActivityThread> {

    private final Time eventTime;

    public EventBin(Time eventTime) {
        this.eventTime = eventTime;
    }

    public void executeAll() {
        for (ActivityThread t: this) {
            // will start or resume the associated ActivityThread
            t.execute();
        }
    }

    public Time getEventTime() {
        return this.eventTime;
    }

}