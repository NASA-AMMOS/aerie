package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A thread-based temporal task execution engine.
 *
 * This engine accepts a single top-level task to perform, which may itself spawn
 * additional tasks scheduled against a simulated timeline. Tasks may pause themselves
 * to be resumed at a later point in time.
 *
 * See {@link SimulationContext} for the fully set of control actions a task may perform.
 */
public final class SimulationEngine {
    /// The simulation time of the currently-executing task.
    private Instant currentSimulationTime;

    /// The control information associated with the currently-executing task.
    private volatile JobContext activeJob = null;

    /// A time-ordered list of tasks to be performed/resumed.
    private PriorityQueue<Pair<Instant, JobContext>> eventQueue = new PriorityQueue<>(Comparator.comparing(Pair::getLeft));

    /// The provider of control effects given to each task.
    private final SimulationContext effectsProvider;

    /// A pool of worker threads which track the current progress of paused tasks.
    private final ExecutorService threadPool = Executors.newCachedThreadPool(r -> {
        // Daemonize all threads in this pool, so that they don't block the application on shutdown.
        // TODO: Properly call `shutdown` on the pool instead, once the pool is provided at construction time.
        final var t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

    private SimulationEngine(final Instant simulationStartTime) {
        this.effectsProvider = new EffectsProvider();
        this.currentSimulationTime = simulationStartTime;
    }

    public static Instant simulate(
        final Instant simulationStartTime,
        final Collection<? extends State<?>> simulationStates,
        final Runnable topLevel
    ) {
        final var engine = new SimulationEngine(simulationStartTime);

        // TODO: don't initialize states in the simulation engine -- there should be no need to couple the simulation
        //   engine to states.
        for (final var state : simulationStates) state.initialize(simulationStartTime);

        engine.run((ctx) -> SimulationEffects.withEffects(ctx, topLevel::run));
        return engine.currentSimulationTime;
    }

    /**
     * Schedules the given task to run immediately, and pumps the event queue to depletion.
     */
    private void run(final Consumer<SimulationContext> topLevel) {
        final var rootJob = new JobContext();
        this.spawnInThread(rootJob, topLevel);
        this.resumeAfter(Duration.ZERO, rootJob);

        // Run until we've handled all outstanding activity events.
        while (!this.eventQueue.isEmpty()) {
            final var event = this.eventQueue.remove();
            final var eventTime = event.getLeft();
            final var job = event.getRight();

            this.currentSimulationTime = eventTime;
            this.activeJob = job;
            job.yieldControl();
            job.takeControl();
            this.activeJob = null;

            if (job.failure != null) {
                throw new RuntimeException(job.failure);
            }
        }

        this.threadPool.shutdown();
    }

    private void spawnInThread(final JobContext job, final Consumer<SimulationContext> scope) {
        this.threadPool.execute(() -> job.start(scope));
    }

    private void resumeAfter(final Duration duration, final JobContext job) {
        this.eventQueue.add(Pair.of(this.currentSimulationTime.plus(duration), job));
    }

    private enum ActivityStatus { NotStarted, InProgress, Complete }

    private final class JobContext {
        private final Object CONTROL_TOKEN = new Object();
        private final SynchronousQueue<Object> channel = new SynchronousQueue<>();

        private final List<JobContext> children = new ArrayList<>();
        private final Set<JobContext> listeners = new HashSet<>();

        // The most recent exception thrown by this activity, if any.
        public volatile Throwable failure = null;
        public volatile ActivityStatus status = ActivityStatus.NotStarted;

        public void start(final Consumer<SimulationContext> scope) {
            this.takeControl();
            try {
                this.status = ActivityStatus.InProgress;
                scope.accept(SimulationEngine.this.effectsProvider);
                this.waitForChildren();
                this.status = ActivityStatus.Complete;

                // Notify any listeners that we've finished.
                for (final var listener : this.listeners) {
                    this.listeners.remove(listener);
                    SimulationEngine.this.resumeAfter(Duration.ZERO, listener);
                }
            } catch (final Throwable ex) {
                this.failure = ex;
                this.status = ActivityStatus.Complete;
            } finally {
                this.yieldControl();
            }
        }

        public void waitForActivity(final JobContext jobToAwait) {
            // handle case where activity is already complete:
            // we don't want to block on it because we will never receive a notification that it is complete
            if (jobToAwait.status == ActivityStatus.Complete) return;

            jobToAwait.listeners.add(this);
            this.yieldControl();
            this.takeControl();
        }

        public void waitForChildren() {
            for (final var child : this.children) this.waitForActivity(child);
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
    }

    private final class EffectsProvider implements SimulationContext {
        @Override
        public SpawnedActivityHandle defer(final Duration duration, final Consumer<SimulationContext> childActivity) {
            final var childActivityJob = new JobContext();
            SimulationEngine.this.activeJob.children.add(childActivityJob);

            SimulationEngine.this.spawnInThread(childActivityJob, childActivity);
            SimulationEngine.this.resumeAfter(duration, childActivityJob);

            return new SpawnedActivityHandle() {
                @Override
                public void await() {
                    // TODO: Check that the awaited activity is not itself the waiting activity?
                    SimulationEngine.this.activeJob.waitForActivity(childActivityJob);
                }
            };
        }

        @Override
        public void delay(Duration duration) {
            if (duration.isNegative()) throw new IllegalArgumentException("Duration must be non-negative");

            final var job = SimulationEngine.this.activeJob;

            SimulationEngine.this.resumeAfter(duration, job);
            job.yieldControl();
            job.takeControl();
        }

        @Override
        public void waitForAllChildren() {
            SimulationEngine.this.activeJob.waitForChildren();
        }

        @Override
        public Instant now() {
            return SimulationEngine.this.currentSimulationTime;
        }
    }
}
