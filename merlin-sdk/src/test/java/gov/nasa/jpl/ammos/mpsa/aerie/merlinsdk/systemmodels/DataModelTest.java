package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.ActivityEffects.delay;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.ActivityEffects.spawn;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.ActivityEffects.waitForChildren;

final class MySystemModel implements SystemModel {
    public final DataModel dataModel;

    public MySystemModel(final Instant initialInstant) {
        this.dataModel = new DataModel(initialInstant);
    }

    public MySystemModel(final MySystemModel other) {
        this.dataModel = new DataModel(other.dataModel);
    }

    public MySystemModel duplicate() {
        return new MySystemModel(this);
    }

    @Override
    public void step(final Duration dt) {
        // Step all models.
        this.dataModel.step(dt);
        // ...
    }

    @Override
    public void react(final String key, final Object stimulus) {
        // Apply the event to the appropriate models.
        if (key.equals("ADD /data/rate")) {
            this.dataModel.accumulateDataRate((Double) stimulus);
        } else if (key.equals("SET /data/protocol")) {
            this.dataModel.setDataProtocol((DataModel.Protocol) stimulus);
        } else {
            throw new RuntimeException("Unknown stimulus with key `" + key + "`");
        }
    }
}

public final class DataModelTest {
    public static void main(final String[] args) {
        final Instant initialInstant = SimulationInstant.origin();

        // Create an event timeline.
        final var timeline = new EventTimeline();
        final var addDataRate = timeline.<Double>createChannel("ADD /data/rate");
        final var setDataProtocol = timeline.<DataModel.Protocol>createChannel("SET /data/protocol");

        // Define a means of getting a system model at a given point in time.
        final var initialSystemModel = new MySystemModel(initialInstant);
        final Function<Instant, MySystemModel> getAtTime = (time) -> {
            final var accumulator = initialSystemModel.duplicate();

            var now = initialInstant;
            for (final var event : timeline) {
                if (event.time.isAfter(time)) break;
                if (event.time.isBefore(now)) continue;

                if (now.isBefore(event.time)) {
                    accumulator.step(now.durationTo(event.time));
                    now = event.time;
                }

                accumulator.react(event.key, event.value);
            }

            return accumulator;
        };

        // Simulate activities.
        final Instant endTime;
        {
            final var queue = new PriorityQueue<Pair<Instant, Runnable>>(Comparator.comparing(Pair::getLeft));

            final var ctx = new Object() {
                private Instant now = initialInstant;

                public void after(final Duration duration, final Runnable action) {
                    if (duration.isNegative()) throw new RuntimeException("Cannot wait for a negative duration");
                    queue.add(Pair.of(this.now.plus(duration), action));
                }

                public void after(final long quantity, final TimeUnit units, final Runnable action) {
                    this.after(Duration.fromQuantity(quantity, units), action);
                }

                public Instant now() {
                    return this.now;
                }
            };

            // Schedule the activities to be simulated.
            {
                // Build time-aware wrappers around mission resources.
                final var dataRate = new Object() {
                    public double get() {
                        return getAtTime.apply(ctx.now()).dataModel.getDataRate();
                    }

                    public void increaseBy(final double delta) {
                        addDataRate.add(ctx.now(), delta);
                    }

                    public void decreaseBy(final double delta) {
                        addDataRate.add(ctx.now(), -delta);
                    }
                };

                final var dataProtocol = new Object() {
                    public DataModel.Protocol get() {
                        return getAtTime.apply(ctx.now()).dataModel.getDataProtocol();
                    }

                    public void set(final DataModel.Protocol protocol) {
                        setDataProtocol.add(ctx.now(), protocol);
                    }
                };

                queue.add(Pair.of(initialInstant, () -> {
                    ThreadedActivityEffects.enter(ctx::after, () -> {
                        spawn(10, TimeUnit.SECONDS, () -> {
                            dataRate.increaseBy(1.0);
                            delay(10, TimeUnit.SECONDS);
                            dataRate.increaseBy(9.0);
                            delay(20, TimeUnit.SECONDS);
                            dataRate.increaseBy(5.0);
                        });
                        spawn(10, TimeUnit.SECONDS, () -> {
                            dataProtocol.set(DataModel.Protocol.Spacewire);
                            delay(30, TimeUnit.SECONDS);
                            dataProtocol.set(DataModel.Protocol.UART);
                        });
                        waitForChildren();
                        delay(1, TimeUnit.SECONDS);
                        dataRate.decreaseBy(15.0);
                        delay(5, TimeUnit.SECONDS);
                        dataRate.increaseBy(10.0);
                    });
                }));
            }

            while (!queue.isEmpty()) {
                final var job = queue.remove();
                ctx.now = job.getLeft();
                job.getRight().run();
            }

            endTime = ctx.now();
        }

        // Analyze the simulation results.
        var system = getAtTime.apply(endTime);

        System.out.println(system.dataModel.getDataRateHistory());
        System.out.println(system.dataModel.getDataProtocolHistory());
        System.out.println(system.dataModel.whenRateGreaterThan(10));

        if (!system.dataModel.whenRateGreaterThan(10).isEmpty()) {
            System.out.println("Oh no! Constraint violated!");
        }
    }
}

final class ActivityEffects {
    private ActivityEffects() {}

    interface Provider {
        void delay(final Duration duration);
        void spawn(final Duration duration, final Runnable activity);
        void waitForChildren();
    }

    // It's best to think of a `ThreadLocal` not as data, but as a dynamically-scoped variable that exists somewhere
    // near the base of a thread's call stack. The actual `ThreadLocal` instance serves only to look up that data
    // in the ambient context of the active thread.
    private static final ThreadLocal<Provider> dynamicProvider = ThreadLocal.withInitial(() -> null);

    public static void enter(final Provider provider, final Runnable scope) {
        final var previous = dynamicProvider.get();

        dynamicProvider.set(provider);
        try {
            scope.run();
        } finally {
            dynamicProvider.set(previous);
        }
    }

    public static void delay(final Duration duration) {
        Objects
            .requireNonNull(dynamicProvider.get(), "delay cannot be called outside of activity context")
            .delay(duration);
    }

    public static void spawn(final Duration duration, final Runnable activity) {
        Objects
            .requireNonNull(dynamicProvider.get(), "spawn cannot be called outside of activity context")
            .spawn(duration, activity);
    }

    public static void waitForChildren() {
        Objects
            .requireNonNull(dynamicProvider.get(), "waitForChildren cannot be called outside of activity context")
            .waitForChildren();
    }

    public static void delay(final long quantity, final TimeUnit units) {
        delay(Duration.fromQuantity(quantity, units));
    }

    public static void spawn(final long quantity, final TimeUnit units, final Runnable activity) {
        spawn(Duration.fromQuantity(quantity, units), activity);
    }
}

final class ThreadedActivityEffects {
    private final BiConsumer<Duration, Runnable> scheduleEvent;

    private ThreadedActivityEffects(final BiConsumer<Duration, Runnable> scheduleEvent) {
        this.scheduleEvent = scheduleEvent;
    }

    public static void enter(final BiConsumer<Duration, Runnable> scheduleEvent, final Runnable scope) {
        final var effects = new ThreadedActivityEffects(scheduleEvent);
        ActivityEffects.enter(
            effects.new Provider(null),
            () -> ActivityEffects.spawn(Duration.ZERO, scope));
    }

    private final class Provider implements ActivityEffects.Provider {
        private final Provider parent;
        private final Set<Provider> uncompletedChildren = new HashSet<>();
        private volatile boolean isActive = false;
        private volatile boolean isWaitingForChildren = false;

        public Provider(final Provider parent) {
            this.parent = parent;
        }

        @Override
        public void delay(final Duration duration) {
            ThreadedActivityEffects.this.scheduleEvent.accept(duration, () -> {
                // Resume the thread.
                this.isActive = true;
                // Wait until the thread has yielded.
                while (this.isActive) {}
            });

            // Yield control back to the coordinator.
            this.isActive = false;
            // Wait until this thread is allowed to continue.
            while (!this.isActive) {}
        }

        @Override
        public void spawn(final Duration duration, final Runnable activity) {
            final var child = new Provider(this);
            this.uncompletedChildren.add(child);

            ThreadedActivityEffects.this.scheduleEvent.accept(duration, () -> {
                final var t = new Thread(() -> {
                    ActivityEffects.enter(child, () -> {
                        try {
                            // Wait until this activity is allowed to continue.
                            while (!child.isActive) {}

                            // Run the activity.
                            activity.run();

                            // Wait for any spawned activities to complete.
                            child.waitForChildren();

                            // Tell the parent that its child has completed.
                            if (child.parent != null) {
                                child.parent.uncompletedChildren.remove(child);
                                if (child.parent.uncompletedChildren.isEmpty() && child.parent.isWaitingForChildren) {
                                    ThreadedActivityEffects.this.scheduleEvent.accept(Duration.ZERO, () -> {
                                        // Mark this activity as no longer waiting for its children.
                                        child.parent.isWaitingForChildren = false;
                                        // Resume the thread.
                                        child.parent.isActive = true;
                                        // Wait until the thread has yielded.
                                        while (child.parent.isActive) {}
                                    });
                                }
                            }
                        } finally {
                            // Yield control back to the coordinator.
                            child.isActive = false;
                        }
                    });
                });
                t.setDaemon(true);
                t.start();

                // Resume the thread.
                child.isActive = true;
                // Wait until the thread has yielded.
                while (child.isActive) {}
            });
        }

        @Override
        public void waitForChildren() {
            if (!this.uncompletedChildren.isEmpty()) {
                // Mark this activity as awaiting its children.
                this.isWaitingForChildren = true;
                // Yield control back to the coordinator.
                this.isActive = false;
                // Wait until this activity is allowed to continue.
                while (!this.isActive) {}
            }
        }
    }
}
