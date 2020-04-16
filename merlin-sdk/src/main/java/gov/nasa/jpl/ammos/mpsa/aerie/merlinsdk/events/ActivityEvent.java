package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

public class ActivityEvent<Activity> implements Event<Activity> {

    private String name;

    /*I think we shouldn't reference an actual activity here instead enforce that each activity has a unique name
    that can be used as an identifier with some sort of lookup table.  Since the current focus is on constraints
    and not refactoring the way we make events, I will do this for now as it is the faster approach.
     */
    private Activity activity;
    private Instant startTime;
    private Duration duration;

    public ActivityEvent(String name, Instant startTime, Duration duration){
        this.name = name;
        this.startTime = startTime;
        this.duration = duration;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Activity value() {
        return this.activity;
    }

    @Override
    public Instant time() {
        return this.startTime;
    }

    public Instant startTime(){
        return time();
    }

    public Instant endTime(){
        return this.startTime.plus(this.duration);
    }

    public Duration duration(){
        return this.duration;
    }

    @Override
    public EventType eventType(){
        return EventType.ACTIVITY;
    }
}
