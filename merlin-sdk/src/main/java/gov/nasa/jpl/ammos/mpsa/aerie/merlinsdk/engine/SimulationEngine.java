package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

/**
 * This class contains the core event loop of a simulation in which activities
 * are dequeued from a time-ordered priority queue and have their effect models
 * run. The underlying architecture is:
 * 
 * Each `Activity` instance has an `ActivityThread` that "owns" it. These
 * threads are organized by time in a `PendingEventQueue` stored within the
 * engine. As it loops through the queue, the engine dequeues these threads and
 * steps forward in time to the event time of each thread. The engine then
 * either starts or resumes the thread (depending on whether it had already
 * started but had `delay()` calls in its effect model). The engine hands
 * execution control over to the activity thread via a `ControlChannel` and
 * blocks until the thread gives that control back. The engine continues this
 * core event loop until the pending event queue is empty.
 * 
 * Additional information that is tracked in `Map`s at the engine level: -
 * activity instances and their owning threads - parent activities and their
 * child activities - activities and their durations (in simulation time) -
 * activities and their listeners (other activities blocking on the key's
 * completion)
 * 
 * @param <T> the type of the adapter-provided state index structure
 */
public class SimulationEngine<T extends StateContainer> {

    // TODO: various detachments/map deletions to encourage garbage collection of
    // old events?

    /**
     * The current simulation time of the engine
     */
    private Time currentSimulationTime;

    /**
     * The priority queue of time-ordered `ActivityThread`s
     */
    private PendingEventQueue<T> pendingEventQueue = new PendingEventQueue<>();

    /**
     * A map of activity instances to their owning threads
     */
    private Map<Activity<T>, ActivityThread<T>> activityToThreadMap = new HashMap<>();

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
     * Initializes the simulation engine
     * 
     * @param simulationStartTime
     * @param activityThreads
     * @param states
     */
    public SimulationEngine(Time simulationStartTime, List<ActivityThread<T>> activityThreads, T states) {
        this.states = states;
        registerStates(this.states.getStateList());

        this.currentSimulationTime = simulationStartTime;

        for (ActivityThread<T> thread : activityThreads) {
            this.pendingEventQueue.add(thread);
            this.activityToThreadMap.put(thread.getActivity(), thread);
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
            ActivityThread<T> thread = pendingEventQueue.remove();
            this.currentSimulationTime = thread.getEventTime();
            this.executeActivity(thread);
        }
    }

    /**
     * Dispatches a `ThreadContext` to a specific activity thread
     * 
     * @param activityThread the activity thread to which the engine should dispatch
     *                       a `ThreadContext`
     */
    public void dispatchContext(ActivityThread<T> activityThread) {
        ThreadContext<T> ctx = new ThreadContext<>(this, activityThread);
        activityThread.setContext(ctx);
    }

    /**
     * Dispatches the engine's state index structure to a specific activity thread
     * 
     * @param activityThread the activity thread to which the engine should dispatch
     *                       states
     */
    public void dispatchStates(ActivityThread<T> activityThread) {
        activityThread.setStates(this.states);
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
     * Executes the effect model of an `ActivityThread`
     * 
     * This method either starts or resumes an activity thread (depending upon if it
     * had already been started and suspended in the past). If the thread needs to
     * be started, this method dispatches a `ThreadContext` and the engine's states
     * to the thread. The engine uses a `ControlChannel` to yield control to the
     * activity thread and block until it returns that control (upon effect model
     * completion OR a delay).
     * 
     * @param thread
     */
    public void executeActivity(ActivityThread<T> activityThread) {
        ControlChannel channel;

        switch (activityThread.getStatus()) {
        case NotStarted:
            this.dispatchContext(activityThread);
            this.dispatchStates(activityThread);
            channel = new ControlChannel();
            activityThread.setChannel(channel);
            activityThread.start();
            break;
        case InProgress:
            channel = activityThread.getChannel();
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
     * Inserts an activity thread into the pending event queue
     * 
     * @param activityThread the thread to be inserted
     */
    public void insertIntoQueue(ActivityThread<T> activityThread) {
        this.pendingEventQueue.add(activityThread);
    }

    /**
     * Registers an activity and its owning thread in the engine's map of said
     * relationships
     * 
     * @param activity       the `Activity` owned by the `ActivityThread`
     * @param activityThread the `ActivityThread` that owns the `Activity`
     */
    public void registerActivityAndThread(Activity<T> activity, ActivityThread<T> activityThread) {
        this.activityToThreadMap.put(activity, activityThread);
    }

    /**
     * Returns the activity thread associated with a given activity
     * 
     * @param activity the `Activity` whose owning thread is desired
     * @return the `ActivityThread` that owns the given activity
     */
    public ActivityThread<T> getActivityThread(Activity<T> activity) {
        return this.activityToThreadMap.get(activity);
    }

    /**
     * Returns the listeners on a given activity thread
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
