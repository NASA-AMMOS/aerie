package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import java.util.function.Function;

final class MyEventLog {
    public final EventTimeline<Channel.Key> timeline = new EventTimeline<>();

    public final Channel<Double> addDataRate = new Channel<>(this.timeline);
    public final Channel<DataModel.Protocol> setDataProtocol = new Channel<>(this.timeline);
}

final class MySystemModel {
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

    public void step(final Duration dt) {
        // Step all models.
        this.dataModel.step(dt);
        // ...
    }

    public void apply(final MyEventLog eventLog, final Channel.Key key) {
        // Apply the event to the appropriate models.
        if (key.channelId == eventLog.addDataRate.id) {
            final var stimulus = eventLog.addDataRate.getStimulusByKey(key);
            this.dataModel.accumulateDataRate(stimulus);
        } else if (key.channelId == eventLog.setDataProtocol.id) {
            final var stimulus = eventLog.setDataProtocol.getStimulusByKey(key);
            this.dataModel.setDataProtocol(stimulus);
        }
    }
}

public final class DataModelTest {
    @Test
    public void testDataModel() {
        final Instant initialInstant = SimulationInstant.origin();

        final var eventLog = new MyEventLog();
        final var initialSystemModel = new MySystemModel(initialInstant);

        final Function<Instant, MySystemModel> getAtTime = (time) -> {
            final var accumulator = initialSystemModel.duplicate();

            var now = initialInstant;
            for (final var event : eventLog.timeline) {
                if (event.time.isAfter(time)) break;
                if (event.time.isBefore(now)) continue;

                if (now.isBefore(event.time)) {
                    accumulator.step(now.durationTo(event.time));
                    now = event.time;
                }

                accumulator.apply(eventLog, event.stimulus);
            }

            return accumulator;
        };

        // Simulate activities.
        final Instant endTime;
        {
            // Keep track of what time it is as we execute the activity.
            final var simClock = new Object() {
                private Instant now = initialInstant;

                public void add(final long quantity, final TimeUnit units) {
                    this.now = this.now.plus(quantity, units);
                }

                public Instant getNow() {
                    return this.now;
                }
            };

            // Build time-aware wrappers around mission resources.
            final var dataRate = new Object() {
                public double get() {
                    return getAtTime.apply(simClock.now).dataModel.getDataRate();
                }

                public void increaseBy(final double delta) {
                    eventLog.addDataRate.scheduleEffect(simClock.getNow(), delta);
                }

                public void decreaseBy(final double delta) {
                    eventLog.addDataRate.scheduleEffect(simClock.getNow(), -delta);
                }
            };

            final var dataProtocol = new Object() {
                public DataModel.Protocol get() {
                    return getAtTime.apply(simClock.now).dataModel.getDataProtocol();
                }

                public void set(final DataModel.Protocol protocol) {
                    eventLog.setDataProtocol.scheduleEffect(simClock.getNow(), protocol);
                }
            };

            // Sequentially build up a timeline of effects.
            {
                simClock.add(10, TimeUnit.SECONDS);
                dataRate.increaseBy(1.0);
                dataProtocol.set(DataModel.Protocol.Spacewire);

                simClock.add(10, TimeUnit.SECONDS);
                dataRate.increaseBy(9.0);

                simClock.add(20, TimeUnit.SECONDS);
                dataRate.increaseBy(5.0);
                dataProtocol.set(DataModel.Protocol.UART);

                simClock.add(1, TimeUnit.SECONDS);
                dataRate.decreaseBy(15.0);

                simClock.add(5, TimeUnit.SECONDS);
                dataRate.increaseBy(10.0);

                simClock.add(1, TimeUnit.MICROSECONDS);

                endTime = simClock.now;
            }
        }

        // Analyze simulated effects.
        var system = getAtTime.apply(endTime);

        // Analyze the simulation results.
        System.out.println(system.dataModel.getDataRateHistory());
        System.out.println(system.dataModel.getDataProtocolHistory());
        System.out.println(system.dataModel.whenRateGreaterThan(10));

        if (!system.dataModel.whenRateGreaterThan(10).isEmpty()) {
            System.out.println("Oh no! Constraint violated!");
        }
    }
}
