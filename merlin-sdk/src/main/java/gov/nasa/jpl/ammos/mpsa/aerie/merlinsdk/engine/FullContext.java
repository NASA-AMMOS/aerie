package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class FullContext<T extends StateContainer> implements SimulationContext<T> {
    
    private final SimulationEngine<T> engine;
    private final ActivityThread<T> activityThread;

    public FullContext(SimulationEngine<T> engine, ActivityThread<T> activityThread) {
        this.engine = engine;
        this.activityThread = activityThread;
    }

    public ActivityThread<T> getActivityThread() {
        return this.activityThread;
    }

    public void delay(Duration d) {
        activityThread.setEventTime(activityThread.getEventTime().add(d));
        engine.insertIntoQueue(activityThread);
        activityThread.suspend();
    }

    public void delayUntil(Time t) {
        activityThread.setEventTime(t);
        engine.insertIntoQueue(activityThread);
        activityThread.suspend();
    }

    public Activity<T> spawnActivity(Activity<T> childActivity) {
        ActivityThread<T> childActivityThread = new ActivityThread<>(childActivity, this.now());
        engine.addParentChildRelationship(activityThread.getActivity(), childActivityThread.getActivity());
        engine.insertIntoQueue(childActivityThread);
        engine.registerActivityAndThread(childActivity, childActivityThread);
        return childActivity;
    }

    public void callActivity(Activity<T> childActivity) {
        ActivityThread<T> childActivityThread = new ActivityThread<>(childActivity, this.now());
        engine.addParentChildRelationship(activityThread.getActivity(), childActivity);
        engine.insertIntoQueue(childActivityThread);
        engine.registerActivityAndThread(childActivity, childActivityThread);
        waitForChild(childActivity);
    }

    public void waitForChild(Activity<T> otherActivity) {
        ActivityThread<T> otherActivityThread = engine.getActivityThread(otherActivity);
        // handle case where activity is already complete:
        // we don't want to block on it because we will never receive a notification that it is complete
        if (otherActivityThread.effectModelIsComplete()) {
            return;
        }
        engine.addActivityListener(otherActivityThread, this.activityThread);
        activityThread.suspend();
    }

    public void waitForAllChildren() {
        try {
            for (Activity<T> child: engine.getActivityChildren(this.activityThread.getActivity())) {
                waitForChild(child);
            }
        } catch (NullPointerException e) {
            // no children
        }
    }

    public void notifyActivityListeners() {
        try {
            for (ActivityThread<T> listener: engine.getActivityListeners(activityThread)) {
                listener.setEventTime(this.now());
                ControlChannel channel = listener.getChannel();
                channel.yieldControl();
                channel.takeControl();
            }
        } catch (NullPointerException e) {
            // no listeners
        }
    }

    public Time now() {
        return engine.getCurrentSimulationTime();
    }

    public void logActivityDuration(Duration d) {
        engine.logActivityDuration(this.activityThread.getActivity(), d);
    }

}