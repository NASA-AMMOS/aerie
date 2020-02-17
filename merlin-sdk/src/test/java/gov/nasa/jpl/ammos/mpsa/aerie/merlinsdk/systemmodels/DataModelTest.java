package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

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
        Instant endTime = initialInstant;
        {
            // Keep track of what time it is as we execute the activity.
            final var ctx = new Object() {
                private Instant now = initialInstant;

                public void after(final long quantity, final TimeUnit units, final Runnable action) {
                    this.now = this.now.plus(quantity, units);
                    action.run();
                }

                public Instant now() {
                    return this.now;
                }

                public MySystemModel getSystemModel() {
                    return getAtTime.apply(this.now);
                }
            };

            // Build time-aware wrappers around mission resources.
            final var dataRate = new Object() {
                public double get() {
                    return ctx.getSystemModel().dataModel.getDataRate();
                }

                public void increaseBy(final double delta) {
                    addDataRate.add(ctx.now(), delta);
                }

                public void decreaseBy(final double delta) {
                    addDataRate.add(ctx.now(), -delta);
                }
            };

            // Define the activity to be simulated.
            final Runnable activity = () -> {
                ctx.after(10, TimeUnit.SECONDS, () -> {
                    dataRate.increaseBy(1.0);

                    ctx.after(10, TimeUnit.SECONDS, () -> {
                        dataRate.increaseBy(9.0);

                        ctx.after(20, TimeUnit.SECONDS, () -> {
                            dataRate.increaseBy(5.0);

                            ctx.after(1, TimeUnit.SECONDS, () -> {
                                dataRate.decreaseBy(15.0);

                                ctx.after(5, TimeUnit.SECONDS, () -> {
                                    dataRate.increaseBy(10.0);
                                });
                            });
                        });
                    });
                });
            };

            // Simulate the activity.
            activity.run();
            endTime = Instant.max(endTime, ctx.now());
        }
        {
            // Keep track of what time it is as we execute the activity.
            final var ctx = new Object() {
                private Instant now = initialInstant;

                public void after(final long quantity, final TimeUnit units, final Runnable action) {
                    this.now = this.now.plus(quantity, units);
                    action.run();
                }

                public Instant now() {
                    return this.now;
                }

                public MySystemModel getSystemModel() {
                    return getAtTime.apply(this.now);
                }
            };

            // Build time-aware wrappers around mission resources.
            final var dataProtocol = new Object() {
                public DataModel.Protocol get() {
                    return ctx.getSystemModel().dataModel.getDataProtocol();
                }

                public void set(final DataModel.Protocol protocol) {
                    setDataProtocol.add(ctx.now(), protocol);
                }
            };

            // Define the activity to be simulated.
            final Runnable activity = () -> {
                ctx.after(10, TimeUnit.SECONDS, () -> {
                    dataProtocol.set(DataModel.Protocol.Spacewire);

                    ctx.after(30, TimeUnit.SECONDS, () -> {
                        dataProtocol.set(DataModel.Protocol.UART);
                    });
                });
            };

            // Simulate the activity.
            activity.run();
            endTime = Instant.max(endTime, ctx.now());
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
