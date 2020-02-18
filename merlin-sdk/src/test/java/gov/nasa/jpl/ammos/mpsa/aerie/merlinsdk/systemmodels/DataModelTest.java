package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import java.util.function.Function;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.ActivityEffects.delay;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.ActivityEffects.now;
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

        // Build time-aware wrappers around mission resources.
        final var dataRate = new Object() {
            public double get() {
                return getAtTime.apply(now()).dataModel.getDataRate();
            }

            public void increaseBy(final double delta) {
                addDataRate.add(now(), delta);
            }

            public void decreaseBy(final double delta) {
                addDataRate.add(now(), -delta);
            }
        };

        final var dataProtocol = new Object() {
            public DataModel.Protocol get() {
                return getAtTime.apply(now()).dataModel.getDataProtocol();
            }

            public void set(final DataModel.Protocol protocol) {
                setDataProtocol.add(now(), protocol);
            }
        };

        // Prepare a schedule of events.
        final Runnable performSchedule = () -> {
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
        };

        // Execute the schedule.
        final Instant endTime = ThreadedActivityEffects.execute(initialInstant, performSchedule);

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
