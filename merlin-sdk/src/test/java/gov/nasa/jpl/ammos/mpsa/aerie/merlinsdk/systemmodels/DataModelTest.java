package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

                final Runnable performSchedule = () -> {
                    enter(new ActivityContext(ctx::after), () -> {
                        spawn(10, TimeUnit.SECONDS, () -> {
                            dataRate.increaseBy(1.0);
                            delay(10, TimeUnit.SECONDS);
                            dataRate.increaseBy(9.0);
                            delay(20, TimeUnit.SECONDS);
                            dataRate.increaseBy(5.0);
                            delay(1, TimeUnit.SECONDS);
                            dataRate.decreaseBy(15.0);
                            delay(5, TimeUnit.SECONDS);
                            dataRate.increaseBy(10.0);
                        });
                        spawn(10, TimeUnit.SECONDS, () -> {
                            dataProtocol.set(DataModel.Protocol.Spacewire);
                            delay(30, TimeUnit.SECONDS);
                            dataProtocol.set(DataModel.Protocol.UART);
                        });
                    });
                };

                ctx.after(0, TimeUnit.SECONDS, performSchedule);
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

    private static final class ActivityContext {
        private final BiConsumer<Duration, Runnable> scheduleEvent;
        private volatile boolean isActive = false;

        public ActivityContext(final BiConsumer<Duration, Runnable> scheduleEvent) {
            this.scheduleEvent = scheduleEvent;
        }
    }

    private static final ThreadLocal<ActivityContext> activityContext = ThreadLocal.withInitial(() -> null);

    public static void enter(final ActivityContext context, final Runnable scope) {
        final var previousContext = activityContext.get();

        activityContext.set(context);
        try {
            scope.run();
        } finally {
            activityContext.set(previousContext);
        }
    }

    public static void delay(final Duration duration) {
        final var self = Objects.requireNonNull(activityContext.get(), "delay cannot be called outside of activity context");

        self.scheduleEvent.accept(duration, () -> {
            // Resume the thread.
            self.isActive = true;
            // Wait until the thread has yielded.
            while (self.isActive) {}
        });

        // Yield control back to the coordinator.
        self.isActive = false;
        // Wait until this thread is allowed to continue.
        while (!self.isActive) {}
    }

    public static void spawn(final Duration duration, final Runnable activity) {
        final var self = Objects.requireNonNull(activityContext.get(), "spawn cannot be called outside of activity context");
        final var child = new ActivityContext(self.scheduleEvent);

        self.scheduleEvent.accept(duration, () -> {
            final var t = new Thread(() -> {
                // Wait until this thread is allowed to continue.
                while (!child.isActive) {}
                // Kick off the activity.
                enter(child, activity);
                // Yield control back to the coordinator.
                child.isActive = false;
            });
            t.setDaemon(true);
            t.start();

            // Resume the thread.
            child.isActive = true;
            // Wait until the thread has yielded.
            while (child.isActive) {}
        });
    }

    public static void delay(final long quantity, final TimeUnit units) {
        delay(Duration.fromQuantity(quantity, units));
    }

    public static void spawn(final long quantity, final TimeUnit units, final Runnable activity) {
        spawn(Duration.fromQuantity(quantity, units), activity);
    }
}
