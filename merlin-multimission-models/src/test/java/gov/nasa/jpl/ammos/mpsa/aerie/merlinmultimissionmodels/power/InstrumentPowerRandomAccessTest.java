package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.delay;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.now;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.withEffects;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * exercises the random-access functionality of the instrument power state
 */
public class InstrumentPowerRandomAccessTest {
    @Test
    public void getAtTimeStartsAtZeroPower() {
        final var simEngine = new SimulationEngine();

        final var state = new InstrumentPower();
        state.initialize(simEngine.getCurrentTime());

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            assertThat(state.get(now())).isCloseTo(0.0, withinPercentage(0.01));
        }));

        simEngine.runToCompletion();
    }

    @Test
    public void getAtTimeSeesContemporarySet() {
        final var simEngine = new SimulationEngine();

        final var state = new InstrumentPower();
        state.initialize(simEngine.getCurrentTime());

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            final double testValue = 300.0;
            state.set(testValue);
            assertThat(state.get(now())).isCloseTo(testValue, withinPercentage(0.01));
        }));

        simEngine.runToCompletion();
    }

    @Test
    public void getAtTimeSeesPastSet() {
        final var simEngine = new SimulationEngine();

        final var state = new InstrumentPower();
        state.initialize(simEngine.getCurrentTime());

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            final double testValue_W = 300.0;
            state.set(testValue_W);

            final var pastTime = now();

            delay(10, TimeUnit.SECONDS);

            assertThat(state.get(pastTime)).isCloseTo(testValue_W, withinPercentage(0.01));
        }));

        simEngine.runToCompletion();
    }

    @Test
    public void getAtTimeSeesPastSetDespiteNewSet() {
        final var simEngine = new SimulationEngine();

        final var state = new InstrumentPower();
        state.initialize(simEngine.getCurrentTime());

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            final double testValue_W = 300.0;
            state.set(testValue_W);

            final var time = now();
            delay(10, TimeUnit.SECONDS);

            state.set(888.0);
            assertThat(state.get(time)).isCloseTo(testValue_W, withinPercentage(0.01));
        }));

        simEngine.runToCompletion();
    }

    @Test
    public void getAtTimeSeesIntermediatePastSet() {
        final var simEngine = new SimulationEngine();

        final var state = new InstrumentPower();
        state.initialize(simEngine.getCurrentTime());

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            final double testValue_W = 300.0;
            state.set(testValue_W);

            delay(10, TimeUnit.SECONDS);
            final var time = now();
            delay(10, TimeUnit.SECONDS);

            state.set(888.0);
            assertThat(state.get(time)).isCloseTo(testValue_W, withinPercentage(0.01));
        }));

        simEngine.runToCompletion();
    }

    @Test
    public void getAtTimeSeesSeriesOfPastSets() {
        final var simEngine = new SimulationEngine();
        final var startTime = simEngine.getCurrentTime();

        final var state = new InstrumentPower();
        state.initialize(startTime);

        final var sets = new TreeMap<>(Map.of(
            startTime.plus(10, TimeUnit.SECONDS), 110.0,
            startTime.plus(20, TimeUnit.SECONDS), 120.0,
            startTime.plus(30, TimeUnit.SECONDS), 130.0
        ));
        final var expecteds = Map.of(
            startTime.plus( 0, TimeUnit.SECONDS), 0.0,
            startTime.plus(10, TimeUnit.SECONDS), 110.0,
            startTime.plus(15, TimeUnit.SECONDS), 110.0,
            startTime.plus(20, TimeUnit.SECONDS), 120.0,
            startTime.plus(25, TimeUnit.SECONDS), 120.0,
            startTime.plus(30, TimeUnit.SECONDS), 130.0,
            startTime.plus(31, TimeUnit.SECONDS), 130.0
        );

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            //run all sets in test vector sequentially (thanks to TreeMap being sorted)
            for (final var set : sets.entrySet()) {
                delay(now().durationTo(set.getKey()));
                state.set(set.getValue());
            }

            delay(now().durationTo(startTime.plus(1, TimeUnit.MINUTES)));

            for (final var expected : expecteds.entrySet()) {
                assertThat(state.get(expected.getKey()))
                    .isCloseTo(expected.getValue(), withinPercentage(0.01));
            }
        }));

        simEngine.runToCompletion();
    }
}
