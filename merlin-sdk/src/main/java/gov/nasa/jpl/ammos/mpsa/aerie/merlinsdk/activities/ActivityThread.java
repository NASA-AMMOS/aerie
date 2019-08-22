package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ControlChannel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ThreadContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

/**
 * A `Runnable` class that wraps a threading layer around an activity instance
 * 
 * Each `ActivityThread` "owns" a particular activity instance so that the engine may appropriately pause and resume
 * effect models when adapters specify delays within them. Each thread also contains auxiliary information that informs
 * the engine of its status (`threadHasStarted`, `effectModelComplete`, etc.).
 * 
 * The `ActivityThread` class also implements the `Comparable` interface so that threads may be ordered in the
 * simulation engine's pending event queue by event time.
 * 
 * @param <T> the type of the adapter-provided state index structure
 */
public class ActivityThread<T extends StateContainer> implements Runnable, Comparable<ActivityThread<T>> {

    // TODO: detach threads for garbage collection?

    /**
     * The start or resumption time of the effect model owned by this thread.
     * 
     * At instantiation: the start time of the activity. Upon delays, this field is updated to reflect the resumption
     * time so that it may be re-inserted into the engine's pending event queue at the appropriate wake-up time. This 
     * value is the index by which the engine's priority queue orders activities.
     */
    private Time eventTime;

    /**
     * ??
     */
    private String name;

    /**
     * The actual `Thread` object that runs this class's `run()` method
     */
    private Thread t;

    /**
     * The activity instance that this thread "owns"
     */
    private Activity<T> activity;

    /**
     * Whether or not the activity's effect model has begun. This will remain true even if delays occur within the
     * effect model.
     */
    private Boolean threadHasStarted = false;

    /**
     * Serves as a bridge between this thread and the engine. It is passed down to the activity's effect model as the
     * `SimulationContext` type to prevent adapter access to certain thread and engine-related methods.
     */
    private ThreadContext<T> ctx;

    /**
     * A means of synchronously passing execution control between this thread and others (primarily the engine).
     */
    private ControlChannel channel;

    /**
     * The adapter-provided state index structure
     */
    private T states;

    /**
     * Whether this thread's activity instance's effect model is complete. This is false throughout the duration of the
     * effect model, and it is only set to true once the activity's effect model is finished AND all of the activity's
     * children's effect models are finished.
     */
    private Boolean effectModelComplete = false;

    public ActivityThread(Activity<T> activityInstance, Time startTime) {
        //TODO: don't use reflection here. get the instance name
        name = activityInstance.getClass().getName();
        activity = activityInstance;
        eventTime = startTime;
        t = new Thread(this, name);
    }

    /**
     * Starts this thread.
     */
    public void start() {
        t.start();
    }

    /**
     * Executes the activity's effect model in coordination with the engine
     * 
     * This method will execute an activity's effect model, block until all child activities are complete, log the
     * duration of the effect model within an engine-level map, and notify the activity threads that are listening on
     * this target. A `ControlChannel` is used to ensure that the engine is not executing at the same time; execution
     * control is explicitly handed back-and-forth between the engine and activity thread via yields and takes.
     */
    public void run() {
        channel.takeControl();

        threadHasStarted = true;
        Time startTime = ctx.now();

        activity.modelEffects(ctx, states);
        ctx.waitForAllChildren();
        effectModelComplete = true;

        Duration activityDuration = ctx.now().subtract(startTime);
        ctx.logActivityDuration(activityDuration);
        ctx.notifyActivityListeners();
        
        channel.yieldControl();
    }

    /**
     * Suspends the activity thread until execution control is explicitly given back to it
     */
    public synchronized void suspend() {
        channel.yieldControl();
        channel.takeControl();
    }
    
    /**
     * Compares `ActivityThread` objects by their event times
     */
    public int compareTo(ActivityThread<T> other) {
        return this.eventTime.compareTo(other.eventTime);
    }

    public Time getEventTime() {
        return this.eventTime;
    }

    public Activity<T> getActivity() {
        return this.activity;
    }

    public void setContext(ThreadContext<T> ctx) {
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

    public void setChannel(ControlChannel channel) {
        this.channel = channel;
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