package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ThreadedActivityEffects;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityEffects.delay;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityEffects.now;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityEffects.spawn;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityEffects.waitForChildren;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit.*;

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
        final var addDataRate = new Object() {
            private final Channel<Double> channel = timeline.createChannel("ADD /data/rate");
            public void add(final double effect) {
                this.channel.add(now(), effect);
            }
        };
        final var setDataProtocol = new Object() {
            private final Channel<DataModel.Protocol> channel = timeline.createChannel("SET /data/protocol");
            public void add(final DataModel.Protocol effect) {
                this.channel.add(now(), effect);
            }
        };

        // Define a means of getting a system model at a given point in time.
        final var initialSystemModel = new MySystemModel(initialInstant);
        final Function<Instant, MySystemModel> getModelAt = (endTime) -> {
            final var accumulator = initialSystemModel.duplicate();

            var currentTime = initialInstant;
            for (final var event : timeline) {
                if (event.time.isBefore(currentTime)) continue;
                if (event.time.isAfter(endTime)) break;

                final var nextTime = endTime.min(event.time);

                accumulator.step(currentTime.durationTo(nextTime));
                accumulator.react(event.key, event.value);

                currentTime = nextTime;
            }

            return accumulator;
        };
        final Supplier<MySystemModel> currentModel = () -> getModelAt.apply(now());

        // Build time-aware wrappers around mission resources.
        final var dataRate = new Object() {
            public double get() {
                return currentModel.get().dataModel.getDataRate();
            }

            public List<Window> whenGreaterThan(final double threshold) {
                return currentModel.get().dataModel.whenRateGreaterThan(threshold);
            }

            public void increaseBy(final double delta) {
                addDataRate.add(delta);
            }

            public void decreaseBy(final double delta) {
                addDataRate.add(-delta);
            }
        };

        final var dataVolume = new Object() {
            public double get() {
                return currentModel.get().dataModel.getDataVolume();
            }
        };

        final var dataProtocol = new Object() {
            public DataModel.Protocol get() {
                return currentModel.get().dataModel.getDataProtocol();
            }

            public void set(final DataModel.Protocol protocol) {
                setDataProtocol.add(protocol);
            }
        };

        // Prepare a schedule of events.
        final Runnable performSchedule = () -> {
            spawn(10, SECONDS, () -> {
                dataRate.increaseBy(1.0);
                delay(10, SECONDS);
                dataRate.increaseBy(9.0);
                delay(20, SECONDS);
                dataRate.increaseBy(5.0);
            });
            spawn(10, SECONDS, () -> {
                dataProtocol.set(DataModel.Protocol.Spacewire);
                delay(30, SECONDS);
                dataProtocol.set(DataModel.Protocol.UART);
            });
            waitForChildren();
            delay(1, SECONDS);
            dataRate.decreaseBy(15.0);
            delay(5, SECONDS);
            dataRate.increaseBy(10.0);
        };

        // Execute the schedule.
        final var endTime = ThreadedActivityEffects.execute(initialInstant, performSchedule);

        // Analyze the simulation results.
        final var system = getModelAt.apply(endTime);

        System.out.println(system.dataModel.getDataRateHistory());
        System.out.println(system.dataModel.getDataProtocolHistory());
        System.out.println(system.dataModel.whenRateGreaterThan(10));

        if (!system.dataModel.whenRateGreaterThan(10).isEmpty()) {
            System.out.println("Oh no! Constraint violated!");
        }
    }
}
