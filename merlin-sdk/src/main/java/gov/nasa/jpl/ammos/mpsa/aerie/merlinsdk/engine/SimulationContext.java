package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class SimulationContext<T extends StateContainer> {
    
    private final SimulationEngine<T> engine;
    private final ActivityThread<T> activityThread;

    public SimulationContext(SimulationEngine<T> engine, ActivityThread<T> activityThread) {
        this.engine = engine;
        this.activityThread = activityThread;
    }

    public ActivityThread<T> getActivityThread() {
        return this.activityThread;
    }

    // TODO: move the queue/map management into the engine itself

    public void delay(Duration d) {
        activityThread.setEventTime(activityThread.getEventTime().add(d));
        engine.pendingEventQueue.add(activityThread);
        activityThread.suspend();
    }

    public void spawnActivity(Activity<T> childActivity) {
        ActivityThread<T> childActivityThread = new ActivityThread<>(childActivity, this.now());
        engine.addParentChildRelationship(activityThread.getActivity(), childActivityThread.getActivity());
        engine.pendingEventQueue.add(childActivityThread);
        engine.activityToThreadMap.put(childActivity, childActivityThread);
    }

    public void spawnActivity(Activity<T> childActivity, Time t) {
        ActivityThread<T> childActivityThread = new ActivityThread<>(childActivity, t);
        engine.addParentChildRelationship(activityThread.getActivity(), childActivityThread.getActivity());
        engine.pendingEventQueue.add(childActivityThread);
        engine.activityToThreadMap.put(childActivity, childActivityThread);
    }

    public void callActivity(Activity<T> childActivity) {
        ActivityThread<T> childActivityThread = new ActivityThread<>(childActivity, this.now());
        engine.addParentChildRelationship(activityThread.getActivity(), childActivity);
        engine.pendingEventQueue.add(childActivityThread);
        engine.activityToThreadMap.put(childActivity, childActivityThread);
        waitForActivity(childActivity);
    }

    public void waitForActivity(Activity<T> otherActivity) {
        // TODO: handle edge case where activity is already complete?
        ActivityThread<T> otherActivityThread = engine.activityToThreadMap.get(otherActivity);
        System.out.println("Adding activity thread '" + activityThread.toString() + "' as listener on target '" + otherActivityThread.toString() + "'");
        engine.addActivityListener(otherActivityThread, this.activityThread);
        activityThread.suspend();
    }

    public void notifyActivityListeners() {
        try {
            for (ActivityThread<T> listener: engine.activityListenerMap.get(activityThread)) {
                System.out.println("Notifying listener '" + listener.toString() + "' of completion");
                listener.reinsertIntoQueue(this.now());
            }
        } catch (NullPointerException e) {
            // no listeners
            System.out.println("No listeners for this activity");
        }
    }

    public Time now() {
        return engine.getCurrentSimulationTime();
    }

    // TODO: refactor this insertion stuff
    public void reinsertActivity() {
        engine.pendingEventQueue.add(activityThread);
    }

}