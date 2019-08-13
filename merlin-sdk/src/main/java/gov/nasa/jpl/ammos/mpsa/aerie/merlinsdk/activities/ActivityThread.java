package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ControlChannel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class ActivityThread<T extends StateContainer> implements Runnable, Comparable<ActivityThread<T>> {

    // TODO: detach threads for garbage collection?
    Time eventTime;
    String name;
    Thread t;
    Activity<T> activity;
    Boolean threadIsSuspended = false;
    SimulationContext<T> ctx;
    ControlChannel channel;
    T states;

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
        System.out.println("ActivityThread took control!");

        activity.modelEffects(ctx, states);
        System.out.println("Effect model complete. Notifying listeners...");
        ctx.notifyActivityListeners();
        
        System.out.println("ActivityThread yielding control!");
        channel.yieldControl();
    }

    public synchronized void suspend() {
        System.out.println("suspend() called");
        threadIsSuspended = true;
        System.out.println("ActivityThread yielding control!");
        channel.yieldControl();
        channel.takeControl();
        System.out.println("ActivityThread took control!");
        threadIsSuspended = false;
    }

    public Time getEventTime() {
        return this.eventTime;
    }

    public Activity<T> getActivity() {
        return this.activity;
    }

    public void join() throws InterruptedException {
        this.t.join();
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

    public void setStates(T states2) {
        this.states = states2;
    }

    public String toString() {
        return this.name;
    }
}