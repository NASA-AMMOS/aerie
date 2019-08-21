package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ControlChannel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class ActivityThread<T extends StateContainer> implements Runnable, Comparable<ActivityThread<T>> {

    // TODO: detach threads for garbage collection?
    private Time eventTime;
    private String name;
    private Thread t;
    private Activity<T> activity;
    private Boolean threadHasStarted = false;
    private SimulationContext<T> ctx;
    private ControlChannel channel;
    private T states;
    private Boolean effectModelComplete = false;

    public ActivityThread(Activity<T> activityInstance, Time startTime) {
        //TODO: don't use reflection here. get the instance name
        name = activityInstance.getClass().getName();
        activity = activityInstance;
        eventTime = startTime;
        t = new Thread(this, name);
    }

    public void execute(ControlChannel channel) {
        this.channel = channel;
        t.start();
    }

    public void run() {
        channel.takeControl();

        threadHasStarted = true;
        Time startTime = ctx.now();

        activity.modelEffects(ctx, states);
        ctx.waitForChildren();
        effectModelComplete = true;

        Duration activityDuration = ctx.now().subtract(startTime);
        ctx.logActivityDuration(activityDuration);
        ctx.notifyActivityListeners();
        
        channel.yieldControl();
    }

    public synchronized void suspend() {
        channel.yieldControl();
        channel.takeControl();
    }

    public Time getEventTime() {
        return this.eventTime;
    }

    public Activity<T> getActivity() {
        return this.activity;
    }

    public int compareTo(ActivityThread<T> other) {
        return this.eventTime.compareTo(other.eventTime);
    }

    public void setContext(SimulationContext<T> ctx) {
        this.ctx = ctx;
    }

    public void setEventTime(Time t) {
        this.eventTime = t;
    }

    public Boolean hasStarted() {
        return this.threadHasStarted;
    }

    public ControlChannel getChannel() {
        return this.channel;
    }

    public String getName() {
        return this.name;
    }

    public void setStates(T states) {
        this.states = states;
    }

    public String toString() {
        return this.name;
    }

    public Boolean effectModelIsComplete() {
        return this.effectModelComplete;
    }
}