package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * exercises the random-access functionality of the instrument power state
 */
public class InstrumentPowerRandomAccessTest {
    private final Instant startTime = SimulationInstant.fromQuantity(0, TimeUnit.SECONDS);

    @Test
    public void getAtTimeStartsAtZeroPower() {
        final var state = new InstrumentPower();
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(StateContainer states) {
                assertThat(state.get(SimulationEffects.now()))
                    .isCloseTo(0.0, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            startTime,
            List.of(new ActivityJob<>(activity, startTime)),
            () -> List.of(state));
    }

    @Test
    public void getAtTimeSeesContemporarySet() {
        final var state = new InstrumentPower();
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(StateContainer states) {
                final double testValue = 300.0;
                state.set(testValue);
                assertThat(state.get(SimulationEffects.now()))
                    .isCloseTo(testValue, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            startTime,
            List.of(new ActivityJob<>(activity, startTime)),
            () -> List.of(state));
    }

    @Test
    public void getAtTimeSeesPastSet() {
        final var state = new InstrumentPower();
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(StateContainer states) {
                final double testValue_W = 300.0;
                state.set(testValue_W);

                SimulationEffects.delay(10, TimeUnit.SECONDS);

                assertThat(state.get(startTime))
                    .isCloseTo(testValue_W, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            startTime,
            List.of(new ActivityJob<>(activity, startTime)),
            () -> List.of(state));
    }

    @Test
    public void getAtTimeSeesPastSetDespiteNewSet() {
        final var state = new InstrumentPower();
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(StateContainer states) {
                final double testValue_W = 300.0;
                state.set(testValue_W);

                SimulationEffects.delay(10, TimeUnit.SECONDS);

                state.set(888.0);
                assertThat(state.get(SimulationEffects.now().minus(10, TimeUnit.SECONDS)))
                    .isCloseTo(testValue_W, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            startTime,
            List.of(new ActivityJob<>(activity, startTime)),
            () -> List.of(state));
    }

    @Test
    public void getAtTimeSeesIntermediatePastSet() {
        final var state = new InstrumentPower();
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(StateContainer states) {
                final double testValue_W = 300.0;
                state.set(testValue_W);

                SimulationEffects.delay(20, TimeUnit.SECONDS);

                state.set(888.0);
                assertThat(state.get(SimulationEffects.now().minus(10, TimeUnit.SECONDS)))
                    .isCloseTo(testValue_W, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            startTime,
            List.of(new ActivityJob<>(activity, startTime)),
            () -> List.of(state));
    }

    @Test
    public void getAtTimeSeesSeriesOfPastSets() {
        final var state = new InstrumentPower();
        final var sets = new TreeMap<>(Map.of(
            startTime.plus(10, TimeUnit.SECONDS), 110.0,
            startTime.plus(20, TimeUnit.SECONDS), 120.0,
            startTime.plus(30, TimeUnit.SECONDS), 130.0
        ));
        final var expecteds =  Map.of(
            startTime.plus( 0, TimeUnit.SECONDS), 0.0,
            startTime.plus(10, TimeUnit.SECONDS), 110.0,
            startTime.plus(15, TimeUnit.SECONDS), 110.0,
            startTime.plus(20, TimeUnit.SECONDS), 120.0,
            startTime.plus(25, TimeUnit.SECONDS), 120.0,
            startTime.plus(30, TimeUnit.SECONDS), 130.0,
            startTime.plus(31, TimeUnit.SECONDS), 130.0
        );

        final var activity = new Activity<>() {
            @Override
            public void modelEffects(final StateContainer _states) {
                //run all sets in test vector sequentially (thanks to TreeMap being sorted)
                for (final var set : sets.entrySet()) {
                    SimulationEffects.delay(set.getKey().durationFrom(SimulationEffects.now()));
                    state.set(set.getValue());
                }

                SimulationEffects.delay(startTime.plus(1, TimeUnit.MINUTES).durationFrom(SimulationEffects.now()));

                for (final var expected : expecteds.entrySet()) {
                    assertThat(state.get(expected.getKey()))
                        .isCloseTo(expected.getValue(), withinPercentage(0.01));
                }
            }
        };

        SimulationEngine.simulate(
            startTime,
            List.of(new ActivityJob<>(activity, startTime)),
            () -> List.of(state));
    }
}
