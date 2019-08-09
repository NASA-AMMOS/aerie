package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

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

    public ActivityThread(Activity<?> activityInstance, Time startTime) {
        name = activityInstance.toString();
        activity = activityInstance;
        eventTime = startTime;
        t = new Thread(this, name);
    }

    public void execute() {
        System.out.println("Calling execute() method");
        if (threadIsSuspended) {
            System.out.println("Resuming from execute() method");
            resume();
        }
        else {
            t.start();
        }
    }

    public void run() {
        activity.modelEffects(ctx);
        ctx.resumeEngine();
    }

    public synchronized void suspend() {
        System.out.println("suspend() called");
        threadIsSuspended = true;
        try {
            wait();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public synchronized void resume() {
        System.out.println("resume() called");
        notify();
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
}