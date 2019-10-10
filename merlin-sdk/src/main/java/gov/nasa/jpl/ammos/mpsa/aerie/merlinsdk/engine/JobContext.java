package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityJob.ActivityStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

/**
 * Functions as a bridge between the simulation engine and an activity job
 * 
 * The `JobContext` is designed to manage the interaction between the `SimulationEngine` and `ActivityJob`
 * objects, allowing for operations like spawning children or delaying effect models from within activities and
 * and ensuring that those operations correctly bubble up to the engine level. This class is also injected into an
 * activity's effect model but under the `SimulationContext` interface. This is to ensure that certain job/thread and
 * engine behaviors (like adding listeners) are exposed to the `ActivityJob` class but NOT to adapters in their
 * effect models.
 * 
 * @param <T> the type of the adapter-provided state index structure
 */
public class JobContext<T extends StateContainer> implements SimulationContext<T> {
    
    /**
     * A reference to the simulation engine that dispatched this context
     */
    private final SimulationEngine<T> engine;

    /**
     * A reference to the activity job to which this context was dispatched
     */
    private final ActivityJob<T> activityJob;

    public JobContext(SimulationEngine<T> engine, ActivityJob<T> activityJob) {
        this.engine = engine;
        this.activityJob = activityJob;
    }

    /**
     * Returns this context's activityJob
     * 
     * @return
     */
    public ActivityJob<T> getActivityJob() {
        return this.activityJob;
    }

    /**
     * Delays an activity job's thread's execution for some duration `d`
     * 
     * This operation alters the event time of the activity job, re-inserts it into the engine's pending event
     * queue, and suspends the job's thread. The thread blocks until the engine de-queues it in future simulation time
     * and resumes it.
     */
    public void delay(Duration d) {
        if (d.totalSeconds() < 0.0) {
            throw new IllegalArgumentException("Duration `d` must be non-negative");
        }
        this.activityJob.setEventTime(this.activityJob.getEventTime().add(d));
        this.engine.insertIntoQueue(this.activityJob);
        this.activityJob.suspend();
    }

    /**
     * Delays an activity job's thread's execution until some time `t`
     * 
     * This operation alters the event time of the activity job, re-inserts it into the engine's pending event
     * queue, and suspends the job's thread. The thread blocks until the engine de-queues it in future simulation time
     * and resumes it.
     */
    public void delayUntil(Time t) {
        if (t.lessThan(this.now())) {
            throw new IllegalArgumentException("Time `t` must occur in the future");
        }
        this.activityJob.setEventTime(t);
        this.engine.insertIntoQueue(this.activityJob);
        this.activityJob.suspend();
    }

    /**
     * Spawns a child activity in the background
     * 
     * This method will create an `ActivityJob` for the given `childActivity` and insert it into the engine's pending
     * event queue at the current simulation time. It also registers the spawning and spawned job as parent and
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
        ActivityJob<T> childActivityJob = new ActivityJob<>(childActivity, this.now());
        this.engine.addParentChildRelationship(this.activityJob.getActivity(), childActivityJob.getActivity());
        this.engine.insertIntoQueue(childActivityJob);
        this.engine.registerActivityAndJob(childActivity, childActivityJob);
        return childActivity;
    }

    /**
     * Spawns a child activity and blocks on the completion of its effect model
     * 
     * This method will create an `ActivityJob` for the given `childActivity` and insert it into the engine's pending
     * event queue at the current simulation time. It registers this activity job as a listener on the child job,
     * and it will block on the child activity until the child's effect model is complete. It also registers the
     * spawning and spawned job as parent and child, respectively, within the engine's map.
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
        ActivityJob<T> childActivityJob = new ActivityJob<>(childActivity, this.now());
        this.engine.addParentChildRelationship(this.activityJob.getActivity(), childActivity);
        this.engine.insertIntoQueue(childActivityJob);
        this.engine.registerActivityAndJob(childActivity, childActivityJob);
        this.waitForChild(childActivity);
        return childActivity;
    }

    /**
     * Blocks a parent activity job on the completion of a child's effect model
     * 
     * @param childActivity the target activity on which to block
     */
    public void waitForChild(Activity<T> childActivity) {
        ActivityJob<T> childActivityJob = engine.getActivityJob(childActivity);
        // handle case where activity is already complete:
        // we don't want to block on it because we will never receive a notification that it is complete
        if (childActivityJob.getStatus() == ActivityStatus.Complete) {
            return;
        }
        this.engine.addActivityListener(childActivity, this.activityJob.getActivity());
        this.activityJob.suspend();
    }

    /**
     * Blocks a parent activity thread on the completion of all of its children
     */
    public void waitForAllChildren() {
        for (Activity<T> child: this.engine.getActivityChildren(this.activityJob.getActivity())) {
            this.waitForChild(child);
        }
    }

    /**
     * Notifies an activity job's listeners that it has completed
     * 
     * This also currently yields control to listeners to their effect models to continue.
     * 
     * TODO: we may want to refactor this and allow for generic listener behavior
     */
    public void notifyActivityListeners() {
        for (Activity<T> listener: this.engine.getActivityListeners(this.activityJob.getActivity())) {
            this.engine.removeActivityListener(this.activityJob.getActivity(), listener);
            
            ActivityJob<T> listenerThread = this.engine.getActivityJob(listener);
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
        this.engine.logActivityDuration(this.activityJob.getActivity(), d);
    }

}
