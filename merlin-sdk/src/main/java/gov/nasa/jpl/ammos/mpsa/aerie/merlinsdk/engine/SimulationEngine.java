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
import java.util.function.Consumer;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

/**
 * This class contains the core event loop of a simulation in which activities
 * are dequeued from a time-ordered priority queue and have their effect models
 * run. The underlying architecture is:
 * 
 * Each `Activity` instance has an `ActivityJob` that "owns" it. These
 * jobs are organized by time in a `PendingEventQueue` stored within the
 * engine. As it loops through the queue, the engine dequeues these jobs and
 * steps forward in time to the event time of each job. The engine then either
 * supplies the activity job to a thread pool to begin execution or resumes the
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
 */
public class SimulationEngine {
    /**
     * The current simulation time of the engine
     */
    private Instant currentSimulationTime;

    /**
     * The priority queue of time-ordered `ActivityJob`s
     */
    private PendingEventQueue pendingEventQueue = new PendingEventQueue();

    /**
     * A map of activity instances to their owning jobs
     */
    private Map<Activity<?>, ActivityJob<?>> activityToJobMap = new HashMap<>();

    /**
     * A map of parent activity instances to their children
     */
    private Map<Activity<?>, List<Activity<?>>> parentChildMap = new HashMap<>();

    /**
     * A map of target activity to their listeners (activities that are blocking on
     * the target's completion)
     */
    private Map<Activity<?>, Set<Activity<?>>> activityListenerMap = new HashMap<>();

    private StateContainer stateContainer;

    /**
     * A thread pool used for executing `ActivityJob`s
     */
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * How often to call the sampling hook during simulation
     *
     * Defaults to never.
     */
    private Duration samplingPeriod = Duration.fromQuantity(0, TimeUnit.MICROSECONDS);

    /**
     * The sampling hook to call every sampling period
     *
     * Defaults to a no-op hook.
     */
    private Consumer<Instant> samplingHook = (now) -> {};

    /**
     * Initializes the simulation engine
     * 
     * @param simulationStartTime
     * @param activityJobs
     * @param stateContainer
     */
    private SimulationEngine(Instant simulationStartTime, List<ActivityJob<?>> activityJobs,
                            StateContainer stateContainer) {
        this.stateContainer = stateContainer;
        this.currentSimulationTime = simulationStartTime;

        for (final var state : stateContainer.getStateList()) {
            state.initialize(simulationStartTime);
        }

        for (ActivityJob<?> job : activityJobs) {
            this.pendingEventQueue.add(job);
            this.activityToJobMap.put(job.getActivity(), job);
        }
    }

    public static Instant simulate(
        final Instant simulationStartTime,
        final List<ActivityJob<?>> activityJobs,
        final StateContainer stateContainer
    ) {
        final var engine = new SimulationEngine(simulationStartTime, activityJobs, stateContainer);
        engine.run();
        return engine.getCurrentSimulationTime();
    }

    public static Instant simulate(
        final Instant simulationStartTime,
        final List<ActivityJob<?>> activityJobs,
        final StateContainer stateContainer,
        final Duration samplingPeriod,
        final Consumer<Instant> samplingHook
    ) {
        final var engine = new SimulationEngine(simulationStartTime, activityJobs, stateContainer);
        engine.setSamplingHook(samplingPeriod, samplingHook);
        engine.run();
        return engine.getCurrentSimulationTime();
    }

    private void setSamplingHook(final Duration samplingPeriod, final Consumer<Instant> samplingHook) {
        if (samplingHook == null || !samplingPeriod.isPositive()) {
            this.samplingPeriod = Duration.fromQuantity(0, TimeUnit.MICROSECONDS);
            this.samplingHook = (now) -> {};
        } else {
            if (this.samplingPeriod.isPositive()) {
                System.err.println("[WARNING] Overriding existing sampling hook");
            }
            this.samplingPeriod = samplingPeriod;
            this.samplingHook = samplingHook;
        }
    }

    /**
     * Performs the main event-loop of linear simulation.
     * 
     * See the class-level docs for more information.
     */
    private void run() {
        var nextSampleTime = this.currentSimulationTime;

        // Run until we've handled all outstanding activity events.
        while (!this.pendingEventQueue.isEmpty()) {
            final ActivityJob<?> job = pendingEventQueue.remove();
            final var eventTime = job.getEventTime();

            // Handle all of the sampling events that occur before the next activity event.
            if (this.samplingPeriod.isPositive()) {
                while (nextSampleTime.isBefore(eventTime)) {
                    this.currentSimulationTime = nextSampleTime;

                    this.samplingHook.accept(this.currentSimulationTime);
                    nextSampleTime = nextSampleTime.plus(this.samplingPeriod);
                }
            }

            this.currentSimulationTime = eventTime;
            this.executeActivity(job);
        }

        if (!nextSampleTime.isAfter(this.currentSimulationTime)) {
            this.samplingHook.accept(this.currentSimulationTime);
        }

        this.threadPool.shutdown();
    }

    /**
     * Returns the engine's current simulation time
     * 
     * @return the current simulation time
     */
    public Instant getCurrentSimulationTime() {
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
    private void executeActivity(ActivityJob<?> activityJob) {
        ControlChannel channel;

        switch (activityJob.getStatus()) {
        case NotStarted:
            activityJob.setContext(new JobContext(activityJob));
            activityJob.setStates(this.stateContainer);
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

    public void spawnActivityFromParent(final Activity<?> child, final Activity<?> parent) {
        final var childActivityJob = new ActivityJob<>(child, this.currentSimulationTime);

        this.parentChildMap.putIfAbsent(parent, new ArrayList<>());
        this.parentChildMap.get(parent).add(child);
        this.pendingEventQueue.add(childActivityJob);
        this.activityToJobMap.put(child, childActivityJob);
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
    public void addActivityListener(Activity<?> target, Activity<?> listener) {
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
    public void removeActivityListener(Activity<?> target, Activity<?> listener) {
        this.activityListenerMap.get(target).remove(listener);
    }

    /**
     * Inserts an activity job into the pending event queue
     * 
     * @param activityJob the job to be inserted
     */
    public void insertIntoQueue(ActivityJob<?> activityJob) {
        this.pendingEventQueue.add(activityJob);
    }

    /**
     * Returns the activity job associated with a given activity
     * 
     * @param activity the `Activity` whose owning job is desired
     * @return the `ActivityJob` that owns the given activity
     */
    public ActivityJob<?> getActivityJob(Activity<?> activity) {
        return this.activityToJobMap.get(activity);
    }

    /**
     * Returns the listeners on a given activity job
     * 
     * @param target the target activity whose completion listeners are blocking on
     * @return a set of the listeners that are blocked on the target's completion
     */
    public Set<Activity<?>> getActivityListeners(Activity<?> target) {
        return Collections.unmodifiableSet(activityListenerMap.getOrDefault(target, Collections.emptySet()));
    }

    /**
     * Returns a list of child activities for a given parent (or null if no children
     * exist)
     * 
     * @param activity the parent activity
     * @return the list of child activities
     */
    public List<Activity<?>> getActivityChildren(Activity<?> activity) {
        return Collections.unmodifiableList(parentChildMap.getOrDefault(activity, Collections.emptyList()));
    }

    /**
     * Functions as a bridge between the simulation engine and an activity job
     *
     * The `JobContext` is designed to manage the interaction between the `SimulationEngine` and `ActivityJob`
     * objects, allowing for operations like spawning children or delaying effect models from within activities and
     * and ensuring that those operations correctly bubble up to the engine level. This class is also injected into an
     * activity's effect model but under the `SimulationContext` interface. This is to ensure that certain job/thread and
     * engine behaviors (like adding listeners) are exposed to the `ActivityJob` class but NOT to adapters in their
     * effect models.
     */
    public final class JobContext implements SimulationContext {
        /**
         * A reference to the activity job to which this context was dispatched
         */
        private final ActivityJob<?> activityJob;

        private JobContext(ActivityJob<?> activityJob) {
            this.activityJob = activityJob;
        }

        /**
         * Delays an activity job's thread's execution for some duration `d`
         *
         * This operation alters the event time of the activity job, re-inserts it into the engine's pending event
         * queue, and suspends the job's thread. The thread blocks until the engine de-queues it in future simulation time
         * and resumes it.
         */
        @Override
        public void delay(Duration d) {
            if (d.isNegative()) {
                throw new IllegalArgumentException("Duration `d` must be non-negative");
            }
            this.activityJob.setEventTime(this.activityJob.getEventTime().plus(d));
            SimulationEngine.this.insertIntoQueue(this.activityJob);
            this.activityJob.suspend();
        }

        /**
         * Delays an activity job's thread's execution until some time `t`
         *
         * This operation alters the event time of the activity job, re-inserts it into the engine's pending event
         * queue, and suspends the job's thread. The thread blocks until the engine de-queues it in future simulation time
         * and resumes it.
         */
        @Override
        public void delayUntil(Instant t) {
            if (t.isBefore(this.now())) {
                throw new IllegalArgumentException("Time `t` must occur in the future");
            }
            this.activityJob.setEventTime(t);
            SimulationEngine.this.insertIntoQueue(this.activityJob);
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
        @Override
        public <T extends StateContainer> Activity<T> spawnActivity(final Activity<T> childActivity) {
            SimulationEngine.this.spawnActivityFromParent(childActivity, this.activityJob.getActivity());
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
        @Override
        public <T extends StateContainer> Activity<T> callActivity(final Activity<T> childActivity) {
            this.spawnActivity(childActivity);
            this.waitForChild(childActivity);
            return childActivity;
        }

        /**
         * Blocks a parent activity job on the completion of a child's effect model
         *
         * @param childActivity the target activity on which to block
         */
        @Override
        public void waitForChild(Activity<?> childActivity) {
            ActivityJob<?> childActivityJob = SimulationEngine.this.getActivityJob(childActivity);
            // handle case where activity is already complete:
            // we don't want to block on it because we will never receive a notification that it is complete
            if (childActivityJob.getStatus() == ActivityJob.ActivityStatus.Complete) {
                return;
            }
            SimulationEngine.this.addActivityListener(childActivity, this.activityJob.getActivity());
            this.activityJob.suspend();
        }

        /**
         * Blocks a parent activity thread on the completion of all of its children
         */
        @Override
        public void waitForAllChildren() {
            for (Activity<?> child: SimulationEngine.this.getActivityChildren(this.activityJob.getActivity())) {
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
            for (Activity<?> listener: SimulationEngine.this.getActivityListeners(this.activityJob.getActivity())) {
                SimulationEngine.this.removeActivityListener(this.activityJob.getActivity(), listener);

                ActivityJob<?> listenerThread = SimulationEngine.this.getActivityJob(listener);
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
        @Override
        public Instant now() {
            return SimulationEngine.this.getCurrentSimulationTime();
        }
    }
}
