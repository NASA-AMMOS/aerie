package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ControlChannel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class ActivityThread implements Runnable, Comparable<ActivityThread> {

    // TODO: detach threads for garbage collection?
    Time eventTime;
    String name;
    Thread t;
    Activity<?> activity;
    Boolean threadIsSuspended = false;
    SimulationContext ctx;
    ControlChannel channel;

    public ActivityThread(Activity<?> activityInstance, Time startTime) {
        name = activityInstance.toString();
        activity = activityInstance;
        eventTime = startTime;
        t = new Thread(this, name);
    }

    public void execute(ControlChannel channel) {
        System.out.println("Calling execute() method");
        this.channel = channel;
        t.start();
    }

    public void run() {
        channel.takeControl();
        System.out.println("ActivityThread took control (in `run()`)!");

        activity.modelEffects(ctx);
        ctx.notifyActivityListeners();
        
        System.out.println("ActivityThread yielding control (in `run()`)!");
        channel.yieldControl();
    }

    public synchronized void suspend() {
        System.out.println("suspend() called");
        threadIsSuspended = true;
        System.out.println("ActivityThread yielding control (in `suspend()`)!");
        channel.yieldControl();
        channel.takeControl();
        System.out.println("ActivityThread took control (in `suspend()`)!");
        threadIsSuspended = false;
    }

    public Time getEventTime() {
        return this.eventTime;
    }

    public Activity<?> getActivity() {
        return this.activity;
    }

    public void join() throws InterruptedException {
        this.t.join();
    }

    public int compareTo(ActivityThread other) {
        return this.eventTime.compareTo(other.eventTime);
    }

    public void setContext(SimulationContext ctx) {
        this.ctx = ctx;
    }

    public void setEventTime(Time t) {
        this.eventTime = t;
    }

    public Boolean isSuspended() {
        return this.threadIsSuspended;
    }

    public ControlChannel getChannel() {
        return this.channel;
    }

    public String getName() {
        return this.name;
    }

    public void reinsertIntoQueue(Time t) {
        this.setEventTime(t);
        ctx.reinsertActivity();
    }
}