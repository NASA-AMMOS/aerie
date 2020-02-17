package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Consumer;
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
                    // TODO: Keep track of the "containing context" for this action, i.e. the owning activity
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
                final class Foo {
                    private volatile boolean threadActive = false;

                    public void delay(final Duration duration) {
                        ctx.after(duration, () -> {
                            // Resume the thread.
                            this.threadActive = true;
                            // Wait until the thread has yielded.
                            while (this.threadActive) {}
                        });

                        // Yield control back to the coordinator.
                        this.threadActive = false;
                        // Wait until this thread is allowed to continue.
                        while (!this.threadActive) {}
                    }

                    public void spawn(final Duration duration, final Consumer<Foo> activity) {
                        final var foo = new Foo();

                        ctx.after(duration, () -> {
                            final var t = new Thread(() -> {
                                // Wait until this thread is allowed to continue.
                                while (!foo.threadActive) {}
                                // Kick off the activity.
                                activity.accept(foo);
                                // Yield control back to the coordinator.
                                foo.threadActive = false;
                            });
                            t.setDaemon(true);
                            t.start();

                            // Resume the thread.
                            foo.threadActive = true;
                            // Wait until the thread has yielded.
                            while (foo.threadActive) {}
                        });
                    }

                    public void delay(final long quantity, final TimeUnit units) {
                        this.delay(Duration.fromQuantity(quantity, units));
                    }

                    public void spawn(final long quantity, final TimeUnit units, final Consumer<Foo> activity) {
                        this.spawn(Duration.fromQuantity(quantity, units), activity);
                    }
                }

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

                final var spawner = new Foo();
                queue.add(Pair.of(initialInstant, () -> {
                    spawner.spawn(10, TimeUnit.SECONDS, (foo) -> {
                        dataRate.increaseBy(1.0);
                        foo.delay(10, TimeUnit.SECONDS);
                        dataRate.increaseBy(9.0);
                        foo.delay(20, TimeUnit.SECONDS);
                        dataRate.increaseBy(5.0);
                        foo.delay(1, TimeUnit.SECONDS);
                        dataRate.decreaseBy(15.0);
                        foo.delay(5, TimeUnit.SECONDS);
                        dataRate.increaseBy(10.0);
                    });
                    spawner.spawn(10, TimeUnit.SECONDS, (foo) -> {
                        dataProtocol.set(DataModel.Protocol.Spacewire);
                        foo.delay(30, TimeUnit.SECONDS);
                        dataProtocol.set(DataModel.Protocol.UART);
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
