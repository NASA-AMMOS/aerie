package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

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
public final class SimulationEngine implements SimulationContext {
    /**
     * The current simulation time of the engine
     */
    private Instant currentSimulationTime;

    /**
     * The job to which any SimulationEffects calls shall be ascribed.
     */
    private JobContext activeJob = null;

    /**
     * The priority queue of time-ordered activity resumption points
     */
    private PriorityQueue<Pair<Instant, JobContext>> pendingEventQueue = new PriorityQueue<>(Comparator.comparing(Pair::getLeft));

    private StateContainer stateContainer;

    /**
     * A thread pool used for executing `ActivityJob`s
     */
    private final ExecutorService threadPool = Executors.newCachedThreadPool(r -> {
        // Daemonize all threads in this pool, so that they don't block the application on shutdown.
        // TODO: Properly call `shutdown` on the pool instead, once the pool is provided at construction time.
        final var t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

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
     * @param activities
     * @param stateContainer
     */
    private <T extends StateContainer>
    SimulationEngine(
        Instant simulationStartTime,
        List<Pair<Instant, ? extends Activity<T>>> activities,
        T stateContainer
    ) {
        this.stateContainer = stateContainer;
        this.currentSimulationTime = simulationStartTime;

        for (final var state : stateContainer.getStateList()) {
            state.initialize(simulationStartTime);
        }

        for (final var entry : activities) {
            final var startTime = entry.getLeft();
            final var activity = entry.getRight();

            final var job = new JobContext(activity);

            this.pendingEventQueue.add(Pair.of(startTime, job));
        }
    }

    public static <T extends StateContainer> Instant simulate(
        final Instant simulationStartTime,
        final List<Pair<Instant, ? extends Activity<T>>> activities,
        final T stateContainer
    ) {
        final var engine = new SimulationEngine(simulationStartTime, activities, stateContainer);
        engine.run();
        return engine.currentSimulationTime;
    }

    public static <T extends StateContainer> Instant simulate(
        final Instant simulationStartTime,
        final List<Pair<Instant, ? extends Activity<T>>> activities,
        final T stateContainer,
        final Duration samplingPeriod,
        final Consumer<Instant> samplingHook
    ) {
        final var engine = new SimulationEngine(simulationStartTime, activities, stateContainer);
        engine.setSamplingHook(samplingPeriod, samplingHook);
        engine.run();
        return engine.currentSimulationTime;
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
            final var event = pendingEventQueue.remove();
            final var eventTime = event.getLeft();
            final var job = event.getRight();

            // Handle all of the sampling events that occur before the next activity event.
            if (this.samplingPeriod.isPositive()) {
                while (nextSampleTime.isBefore(eventTime)) {
                    this.currentSimulationTime = nextSampleTime;

                    this.samplingHook.accept(this.currentSimulationTime);
                    nextSampleTime = nextSampleTime.plus(this.samplingPeriod);
                }
            }

            this.currentSimulationTime = eventTime;
            this.activeJob = job;
            this.executeActivity(job);
            this.activeJob = null;
        }

        if (!nextSampleTime.isAfter(this.currentSimulationTime)) {
            this.samplingHook.accept(this.currentSimulationTime);
        }

        this.threadPool.shutdown();
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
    private void executeActivity(JobContext activityJob) {
        switch (activityJob.status) {
        case NotStarted:
            this.threadPool.execute(() -> SimulationEffects.withEffects(this, activityJob::start));
            activityJob.resume();
            break;
        case InProgress:
            activityJob.resume();
            break;
        case Complete:
            throw new IllegalStateException("Completed activity is somehow in the pending event queue.");
        default:
            throw new IllegalStateException("Unknown activity status");
        }
    }

    private JobContext spawnJobContext(final Activity<?> child) {
        final var childActivityJob = new JobContext(child);

        this.activeJob.children.add(childActivityJob);

        this.pendingEventQueue.add(Pair.of(this.currentSimulationTime, childActivityJob));

        return childActivityJob;
    }

    private void delay(final Instant resumeTime) {
        if (resumeTime.isBefore(this.currentSimulationTime)) {
            throw new IllegalArgumentException("Resumption time must occur in the future");
        }

        this.pendingEventQueue.add(Pair.of(resumeTime, this.activeJob));

        this.activeJob.yield();
    }

    private void waitForActivity(final JobContext jobToAwait) {
        // handle case where activity is already complete:
        // we don't want to block on it because we will never receive a notification that it is complete
        if (jobToAwait.status == ActivityStatus.Complete) return;

        jobToAwait.listeners.add(this.activeJob);

        this.activeJob.yield();
    }

    @Override
    public SpawnedActivityHandle spawnActivity(Activity<?> childActivity) {
        final var childActivityJob = this.spawnJobContext(childActivity);

        return new SimulationContext.SpawnedActivityHandle() {
            @Override
            public void await() {
                SimulationEngine.this.waitForActivity(childActivityJob);
            }
        };
    }

    @Override
    public void delay(Duration duration) {
        if (duration.isNegative()) throw new IllegalArgumentException("Duration must be non-negative");
        this.delay(this.currentSimulationTime.plus(duration));
    }

    @Override
    public void waitForAllChildren() {
        for (final var child : this.activeJob.children) this.waitForActivity(child);
    }

    @Override
    public Instant now() {
        return this.currentSimulationTime;
    }

    public enum ActivityStatus { NotStarted, InProgress, Complete }

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
    private final class JobContext {
        private final ControlChannel channel = new ControlChannel();

        public final Activity<?> activity;
        public final List<JobContext> children = new ArrayList<>();
        public final Set<JobContext> listeners = new HashSet<>();
        public ActivityStatus status = ActivityStatus.NotStarted;

        public JobContext(final Activity<?> activity) {
            this.activity = activity;
        }

        public void start() {
            this.channel.takeControl();

            this.status = ActivityStatus.InProgress;
            ((Activity<StateContainer>)this.activity).modelEffects(SimulationEngine.this.stateContainer);
            SimulationEffects.waitForChildren();
            this.status = ActivityStatus.Complete;

            // Notify any listeners that we've finished.
            for (final var listener : this.listeners) {
                this.listeners.remove(listener);
                SimulationEngine.this.pendingEventQueue.add(Pair.of(SimulationEngine.this.currentSimulationTime, listener));
            }

            this.channel.yieldControl();
        }

        public void yield() {
            this.channel.yieldControl();
            this.channel.takeControl();
        }

        public void resume() {
            this.channel.yieldControl();
            this.channel.takeControl();
        }
    }
}
