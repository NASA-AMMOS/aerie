package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
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
public final class SimulationEngine {
    /**
     * The current simulation time of the engine
     */
    private Instant currentSimulationTime;

    /**
     * The priority queue of time-ordered activity resumption points
     */
    private PriorityQueue<Pair<Instant, JobContext>> eventQueue = new PriorityQueue<>(Comparator.comparing(Pair::getLeft));

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
    private Duration samplingPeriod = Duration.ZERO;

    /**
     * The sampling hook to call every sampling period
     *
     * Defaults to a no-op hook.
     */
    private Consumer<Instant> samplingHook = (now) -> {};

    private <T extends StateContainer> SimulationEngine(
        final Instant simulationStartTime,
        final Runnable topLevel,
        final T stateContainer
    ) {
        this.stateContainer = stateContainer;
        this.currentSimulationTime = simulationStartTime;

        for (final var state : stateContainer.getStateList()) {
            state.initialize(simulationStartTime);
        }

        final var rootJob = new JobContext();
        SimulationEngine.this.spawnInThread(rootJob, topLevel);
        SimulationEngine.this.resumeAfter(Duration.ZERO, rootJob);
    }

    public static <T extends StateContainer> Instant simulate(
        final Instant simulationStartTime,
        final T stateContainer,
        final Runnable topLevel
    ) {
        final var engine = new SimulationEngine(simulationStartTime, topLevel, stateContainer);
        engine.run();
        return engine.currentSimulationTime;
    }

    public static <T extends StateContainer> Instant simulate(
        final Instant simulationStartTime,
        final T stateContainer,
        final Runnable topLevel,
        final Duration samplingPeriod,
        final Consumer<Instant> samplingHook
    ) {
        final var engine = new SimulationEngine(simulationStartTime, topLevel, stateContainer);
        engine.setSamplingHook(samplingPeriod, samplingHook);
        engine.run();
        return engine.currentSimulationTime;
    }

    private void setSamplingHook(final Duration samplingPeriod, final Consumer<Instant> samplingHook) {
        if (samplingHook == null || !samplingPeriod.isPositive()) {
            this.samplingPeriod = Duration.ZERO;
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
        while (!this.eventQueue.isEmpty()) {
            final var event = eventQueue.remove();
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
            job.yieldControl();
            job.takeControl();
        }

        if (!nextSampleTime.isAfter(this.currentSimulationTime)) {
            this.samplingHook.accept(this.currentSimulationTime);
        }

        this.threadPool.shutdown();
    }

    private void spawnInThread(final JobContext job, final Runnable effectsModel) {
        this.threadPool.execute(() -> job.start(effectsModel));
    }

    private void resumeAfter(final Duration duration, final JobContext job) {
        this.eventQueue.add(Pair.of(this.currentSimulationTime.plus(duration), job));
    }

    private enum ActivityStatus { NotStarted, InProgress, Complete }

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
    private final class JobContext implements SimulationContext {
        private final SynchronousQueue<Object> channel = new SynchronousQueue<>();
        private final Object CONTROL_TOKEN = new Object();

        private final List<JobContext> children = new ArrayList<>();
        private final Set<JobContext> listeners = new HashSet<>();

        public ActivityStatus status = ActivityStatus.NotStarted;

        public void start(final Runnable effectsModel) {
            this.takeControl();

            this.status = ActivityStatus.InProgress;
            SimulationEffects.withEffects(this, () -> effectsModel.run());
            this.waitForAllChildren();
            this.status = ActivityStatus.Complete;

            // Notify any listeners that we've finished.
            for (final var listener : this.listeners) {
                this.listeners.remove(listener);
                SimulationEngine.this.eventQueue.add(Pair.of(SimulationEngine.this.currentSimulationTime, listener));
            }

            this.yieldControl();
        }

        public void yieldControl() {
            while (true) {
                try {
                    this.channel.put(CONTROL_TOKEN);
                    break;
                } catch (final InterruptedException ex) {
                    continue;
                }
            }
        }

        public void takeControl() {
            while (true) {
                try {
                    this.channel.take();
                    break;
                } catch (final InterruptedException ex) {
                    continue;
                }
            }
        }

        @Override
        public SpawnedActivityHandle defer(final Duration duration, final Activity<?> childActivity) {
            final var childActivityJob = new JobContext();
            this.children.add(childActivityJob);

            final Runnable effectsModel = () -> ((Activity<StateContainer>)childActivity).modelEffects(SimulationEngine.this.stateContainer);
            SimulationEngine.this.spawnInThread(childActivityJob, effectsModel);
            SimulationEngine.this.resumeAfter(duration, childActivityJob);

            return new SpawnedActivityHandle() {
                @Override
                public void await() {
                    JobContext.this.waitForActivity(childActivityJob);
                }
            };
        }

        @Override
        public void delay(Duration duration) {
            if (duration.isNegative()) throw new IllegalArgumentException("Duration must be non-negative");

            SimulationEngine.this.resumeAfter(duration, this);
            this.yieldControl();
            this.takeControl();
        }

        private void waitForActivity(final JobContext jobToAwait) {
            // handle case where activity is already complete:
            // we don't want to block on it because we will never receive a notification that it is complete
            if (jobToAwait.status == ActivityStatus.Complete) return;

            jobToAwait.listeners.add(this);
            this.yieldControl();
            this.takeControl();
        }

        @Override
        public void waitForAllChildren() {
            for (final var child : this.children) this.waitForActivity(child);
        }

        @Override
        public Instant now() {
            return SimulationEngine.this.currentSimulationTime;
        }
    }
}
