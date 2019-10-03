package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

/**
 * This class contains the core event loop of a simulation in which activities
 * are dequeued from a time-ordered priority queue and have their effect models
 * run. The underlying architecture is:
 * 
 * Each `Activity` instance has an `ActivityJob` that "owns" it. These
 * jobs are organized by time in a `PendingEventQueue` stored within the
 * engine. As it loops through the queue, the engine dequeues these jobs and
 * steps forward in time to the event time of each job. The engine then either
 * supplies the actvitiy job to a thread pool to begin execution or resumes the
 * thread's execution (if it had already started but had `delay()` calls in its
 * effect model). The engine hands execution control over to the activity job's
 * thread via a `ControlChannel` and blocks until the thread gives that control
 * back. The engine continues this core event loop until the pending event queue
 * is empty.
 * 
 * Additional information that is tracked in `Map`s at the engine level:
 * - activity instances and their owning jobs - parent activities and their child activities
 * - activities and their durations (in simulation time)
 * - activities and their listeners (other activities blocking on the key's completion)
 * 
 * @param <T> the type of the adapter-provided state index structure
 */
public class SimulationEngine<T extends StateContainer> {

    /**
     * The current simulation time of the engine
     */
    private Time currentSimulationTime;

    /**
     * The priority queue of time-ordered `ActivityJob`s
     */
    private PendingEventQueue<T> pendingEventQueue = new PendingEventQueue<>();

    /**
     * A map of activity instances to their owning jobs
     */
    private Map<Activity<T>, ActivityJob<T>> activityToJobMap = new HashMap<>();

    /**
     * A map of parent activity instances to their children
     */
    private Map<Activity<T>, List<Activity<T>>> parentChildMap = new HashMap<>();

    /**
     * A map of activity instances to their durations (the length of the effect
     * model in simulation time)
     */
    private Map<Activity<T>, Duration> activityDurationMap = new HashMap<>();

    /**
     * A map of target activity to their listeners (activities that are blocking on
     * the target's completion)
     */
    private Map<Activity<T>, Set<Activity<T>>> activityListenerMap = new HashMap<>();

    /**
     * The thread in which the simulation engine is running
     */
    private Thread engineThread;

    /**
     * The adapter-provided state index structure
     */
    private T states;

    /**
     * A thread pool used for executing `ActivityJob`s
     */
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * Initializes the simulation engine
     * 
     * @param simulationStartTime
     * @param activityJobs
     * @param states
     */
    public SimulationEngine(Time simulationStartTime, List<ActivityJob<T>> activityJobs, T states) {
        this.states = states;
        registerStates(this.states.getStateList());

        this.currentSimulationTime = simulationStartTime;

        for (ActivityJob<T> job : activityJobs) {
            this.pendingEventQueue.add(job);
            this.activityToJobMap.put(job.getActivity(), job);
        }
    }

    /**
     * Performs the main event-loop of linear simulation.
     * 
     * See the class-level docs for more information.
     */
    public void simulate() {
        this.engineThread = Thread.currentThread();

        while (!this.pendingEventQueue.isEmpty()) {
            ActivityJob<T> job = pendingEventQueue.remove();
            this.currentSimulationTime = job.getEventTime();
            this.executeActivity(job);
        }
    }

    /**
     * Dispatches a `JobContext` to a specific activity job
     * 
     * @param activityJob the activity job to which the engine should dispatch
     *                    a `JobContext`
     */
    public void dispatchContext(ActivityJob<T> activityJob) {
        JobContext<T> ctx = new JobContext<>(this, activityJob);
        activityJob.setContext(ctx);
    }

    /**
     * Dispatches the engine's state index structure to a specific activity job
     * 
     * @param activityJob the activity job to which the engine should dispatch
     *                    states
     */
    public void dispatchStates(ActivityJob<T> activityJob) {
        activityJob.setStates(this.states);
    }

    /**
     * Returns the engine's current simulation time
     * 
     * @return the current simulation time
     */
    public Time getCurrentSimulationTime() {
        return this.currentSimulationTime;
    }

    /**
     * Executes the effect model of an `ActivityJob`
     *
     * This method either starts or resumes an activity job (depending upon if it
     * had already been started and suspended in the past). If the job needs to
     * be started, this method dispatches a `JobContext` and the engine's states
     * to the job. The engine uses a `ControlChannel` to yield control to the
     * activity job's thread and block until it returns that control (upon effect model
     * completion OR a delay).
     * 
     * @param activityJob the activity job to start or resume
     */
    public void executeActivity(ActivityJob<T> activityJob) {
        ControlChannel channel;

        switch (activityJob.getStatus()) {
        case NotStarted:
            this.dispatchContext(activityJob);
            this.dispatchStates(activityJob);
            channel = new ControlChannel();
            activityJob.setChannel(channel);

            threadPool.execute( () -> {
                activityJob.execute();
            });
            break;
        case InProgress:
            channel = activityJob.getChannel();
            break;
        case Complete:
            throw new IllegalStateException("Completed activity is somehow in the pending event queue.");
        default:
            throw new IllegalStateException("Unknown activity status");
        }
        channel.yieldControl();
        channel.takeControl();
    }

    /**
     * Adds a parent-child relationship to the engine's map of said relationships
     * 
     * @param parent the parent activity that is decomposing into the child
     * @param child  the child activity into which the parent is decomposing
     */
    public void addParentChildRelationship(Activity<T> parent, Activity<T> child) {
        this.parentChildMap.putIfAbsent(parent, new ArrayList<>());
        this.parentChildMap.get(parent).add(child);
    }

    /**
     * Adds a target-listener relationship to the engine's map of said relationships
     * 
     * Blocked listener activities will be notified upon the target activity's
     * completion, giving the listeners the opportunity to resume their effect
     * models.
     * 
     * @param target   the activity whose completion the listener is blocking
     *                 against
     * @param listener the activity that is blocked until the target's effect model
     *                 completes
     */
    public void addActivityListener(Activity<T> target, Activity<T> listener) {
        this.activityListenerMap.putIfAbsent(target, new HashSet<>());
        this.activityListenerMap.get(target).add(listener);
    }

    /**
     * Removes a target-listener relationship from the engine's map of said relationships
     * 
     * @param target   the activity whose completion the listener is blocking
     *                 against
     * @param listener the activity that is blocked until the target's effect model
     *                 completes
     */
    public void removeActivityListener(Activity<T> target, Activity<T> listener) {
        this.activityListenerMap.get(target).remove(listener);
    }

    /**
     * Given a list of states, registers the engine in each state
     * 
     * @param stateList the list of states to be registered
     */
    public void registerStates(List<SettableState<?>> stateList) {
        for (SettableState<?> state : stateList) {
            state.setEngine(this);
        }
    }

    /**
     * Inserts an activity job into the pending event queue
     * 
     * @param activityJob the job to be inserted
     */
    public void insertIntoQueue(ActivityJob<T> activityJob) {
        this.pendingEventQueue.add(activityJob);
    }

    /**
     * Registers an activity and its owning job in the engine's map of said
     * relationships
     * 
     * @param activity    the `Activity` owned by the `ActivityJob`
     * @param activityJob the `ActivityJob` that owns the `Activity`
     */
    public void registerActivityAndJob(Activity<T> activity, ActivityJob<T> activityJob) {
        this.activityToJobMap.put(activity, activityJob);
    }

    /**
     * Returns the activity job associated with a given activity
     * 
     * @param activity the `Activity` whose owning job is desired
     * @return the `ActivityJob` that owns the given activity
     */
    public ActivityJob<T> getActivityJob(Activity<T> activity) {
        return this.activityToJobMap.get(activity);
    }

    /**
     * Returns the listeners on a given activity job
     * 
     * @param target the target activity whose completion listeners are blocking on
     * @return a set of the listeners that are blocked on the target's completion
     */
    public Set<Activity<T>> getActivityListeners(Activity<T> target) {
        return Collections.unmodifiableSet(activityListenerMap.getOrDefault(target, Collections.emptySet()));
    }

    /**
     * Returns a list of child activities for a given parent (or null if no children
     * exist)
     * 
     * @param activity the parent activity
     * @return the list of child activities
     */
    public List<Activity<T>> getActivityChildren(Activity<T> activity) {
        return Collections.unmodifiableList(parentChildMap.getOrDefault(activity, Collections.emptyList()));
    }

    /**
     * Logs the duration of an activity instance in the engine's map of activities
     * and durations
     * 
     * @param activity the activity instance modeled in the simulation
     * @param d        the length in simulation time of the activity's effect model
     */
    public void logActivityDuration(Activity<T> activity, Duration d) {
        this.activityDurationMap.put(activity, d);
    }

    /**
     * Returns the duration of an activity instance from the engine's map of
     * activities and durations
     * 
     * @param activity the activity instance whose duration is desired
     * @return the length in simulation time of that activity's effect model
     */
    public Duration getActivityDuration(Activity<T> activity) {
        return this.activityDurationMap.get(activity);
    }

}
