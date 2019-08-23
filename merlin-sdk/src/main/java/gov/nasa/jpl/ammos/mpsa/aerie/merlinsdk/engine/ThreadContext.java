package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread.ActivityStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

/**
 * Functions as a bridge between the simulation engine and activity thread
 * 
 * The `ThreadContext` is designed to manage the interaction between the `SimulationEngine` and `ActivityThread`
 * objects, allowing for operations like spawning children or delaying effect models from within activities and
 * and ensuring that those operations correctly bubble up to the engine level. This class is also injected into an
 * activity's effect model but under the `SimulationContext` interface. This is to ensure that certain thread and
 * engine behaviors (like adding listeners) are exposed to the `ActivityThread` class but NOT to adapters in their
 * effect models.
 * 
 * @param <T> the type of the adapter-provided state index structure
 */
public class ThreadContext<T extends StateContainer> implements SimulationContext<T> {
    
    /**
     * A reference to the simulation engine that dispatched this context
     */
    private final SimulationEngine<T> engine;

    /**
     * A reference to the activity thread to which this context was dispatched
     */
    private final ActivityThread<T> activityThread;

    public ThreadContext(SimulationEngine<T> engine, ActivityThread<T> activityThread) {
        this.engine = engine;
        this.activityThread = activityThread;
    }

    /**
     * Returns this context's activityThread
     * 
     * @return
     */
    public ActivityThread<T> getActivityThread() {
        return this.activityThread;
    }

    /**
     * Delays an activity thread's execution for some duration `d`
     * 
     * This operation alters the event time of the activity thread, re-inserts it into the engine's pending event
     * queue, and suspends the thread. The thread blocks until the engine de-queues it in future simulation time and
     * resumes it.
     */
    public void delay(Duration d) {
        if (d.totalSeconds() < 0.0) {
            throw new IllegalArgumentException("Duration `d` must be non-negative");
        }
        this.activityThread.setEventTime(this.activityThread.getEventTime().add(d));
        this.engine.insertIntoQueue(this.activityThread);
        this.activityThread.suspend();
    }

    /**
     * Delays an activity thread's execution until some time `t`
     * 
     * This operation alters the event time of the activity thread, re-inserts it into the engine's pending event
     * queue, and suspends the thread. The thread blocks until the engine de-queues it in future simulation time and
     * resumes it.
     */
    public void delayUntil(Time t) {
        if (t.lessThan(this.now())) {
            throw new IllegalArgumentException("Time `t` must occur in the future");
        }
        this.activityThread.setEventTime(t);
        this.engine.insertIntoQueue(this.activityThread);
        this.activityThread.suspend();
    }

    /**
     * Spawns a child activity in the background
     * 
     * This method will create an `ActivityThread` for the given `childActivity` and insert it into the engine's pending
     * event queue at the current simulation time. It also registers the spawning and spawned thread as parent and
     * child, respectively, within the engine's map. This method does NOT block until the child's effect model is
     * complete. If that behavior is desired, see `callActivity()`.
     * 
     * This method returns the input `childActivity` to allow the user to instantiate the activity in-line with the
     * spawn call, store the activity in a variable, and block on it later if desirable.
     * 
     * @param childActivity the child activity that should be spawned in the background at the current simulation time
     * @return the input child activity
     */
    public Activity<T> spawnActivity(Activity<T> childActivity) {
        ActivityThread<T> childActivityThread = new ActivityThread<>(childActivity, this.now());
        this.engine.addParentChildRelationship(this.activityThread.getActivity(), childActivityThread.getActivity());
        this.engine.insertIntoQueue(childActivityThread);
        this.engine.registerActivityAndThread(childActivity, childActivityThread);
        return childActivity;
    }

    /**
     * Spawns a child activity and blocks on the completion of its effect model
     * 
     * This method will create an `ActivityThread` for the given `childActivity` and insert it into the engine's pending
     * event queue at the current simulation time. It registers this activity thread as a listener on the child thread,
     * and it will block on the child activity until the child's effect model is complete. It also registers the
     * spawning and spawned thread as parent and child, respectively, within the engine's map.
     * 
     * If non-blocking behavior is desired, see `spawnActivity()`.
     * 
     * This method returns the input `childActivity` to allow the user to instantiate the activity in-line with the
     * `callActivity()` call and store it in a variable for later usage.
     * 
     * @param childActivity the child activity that should be spawned and blocked on
     * @return the input child activity
     */
    public Activity<T> callActivity(Activity<T> childActivity) {
        ActivityThread<T> childActivityThread = new ActivityThread<>(childActivity, this.now());
        this.engine.addParentChildRelationship(this.activityThread.getActivity(), childActivity);
        this.engine.insertIntoQueue(childActivityThread);
        this.engine.registerActivityAndThread(childActivity, childActivityThread);
        this.waitForChild(childActivity);
        return childActivity;
    }

    /**
     * Blocks a parent activity thread on the completion of a child's effect model
     * 
     * @param childActivity the target activity on which to block
     */
    public void waitForChild(Activity<T> childActivity) {
        ActivityThread<T> childActivityThread = engine.getActivityThread(childActivity);
        // handle case where activity is already complete:
        // we don't want to block on it because we will never receive a notification that it is complete
        if (childActivityThread.getStatus() == ActivityStatus.Complete) {
            return;
        }
        this.engine.addActivityListener(childActivity, this.activityThread.getActivity());
        this.activityThread.suspend();
    }

    /**
     * Blocks a parent activity thread on the completion of all of its children
     */
    public void waitForAllChildren() {
        for (Activity<T> child: this.engine.getActivityChildren(this.activityThread.getActivity())) {
            this.waitForChild(child);
        }
    }

    /**
     * Notifies an activity thread's listeners that it has completed
     * 
     * This also currently yields control to listeners to their effect models to continue.
     * 
     * TODO: we may want to refactor this and allow for generic listener behavior
     */
    public void notifyActivityListeners() {
        for (Activity<T> listener: this.engine.getActivityListeners(this.activityThread.getActivity())) {
            this.engine.removeActivityListener(this.activityThread.getActivity(), listener);
            
            ActivityThread<T> listenerThread = this.engine.getActivityThread(listener);
            listenerThread.setEventTime(this.now());
            
            ControlChannel channel = listenerThread.getChannel();
            channel.yieldControl();
            channel.takeControl();
        }
    }

    /**
     * Returns the engine's current simulation time
     * 
     * @return current simulation time
     */
    public Time now() {
        return this.engine.getCurrentSimulationTime();
    }

    /**
     * Logs the duration of an activity instance in the engine's map of activities and durations
     * 
     * @param d the length in simulation time of the activity's effect model
     */
    public void logActivityDuration(Duration d) {
        this.engine.logActivityDuration(this.activityThread.getActivity(), d);
    }

}
