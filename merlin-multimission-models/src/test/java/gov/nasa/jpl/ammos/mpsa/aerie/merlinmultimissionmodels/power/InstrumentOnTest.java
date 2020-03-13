package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * unit tests of the multi-mission instrument power on activity
 *
 * these unit tests do not run the actual merlin simulation engine
 */
public class InstrumentOnTest {
    @Test
    public void defaultCtorWorks() {
        new InstrumentOn<>();
    }

    @Test
    public void argCtorWorks() {
        final var powerState_W = new InstrumentPower();
        new InstrumentOn<>(100.0, powerState_W);
    }

    @Test
    public void validationWorks() {
        final var powerState_W = new InstrumentPower();
        final var onAct = new InstrumentOn<>(100.0, powerState_W);

        assertThat(onAct.validateParameters()).isEmpty();
    }

    @Test
    public void validationFailsOnNegativePower() {
        final var powerState_W = new InstrumentPower();
        final var onAct = new InstrumentOn<>(-5.0, powerState_W);

        assertThat(onAct.validateParameters())
            .hasSize(1)
            .usingElementComparator((a, b) -> a.contains(b) ? 0 : 1)
            .contains("negative");
    }

    @Test
    public void validationFailsOnNullState() {
        final var onAct = new InstrumentOn<>(100.0, null);

        assertThat(onAct.validateParameters())
            .hasSize(1)
            .usingElementComparator((a, b) -> a.contains(b) ? 0 : 1)
            .contains("null");
    }

    @Test
    public void validationFailsOnBothNegativePowerAndNullState() {
        final var onAct = new InstrumentOn<>(-5.0, null);

        assertThat(onAct.validateParameters())
            .hasSize(2)
            .usingElementComparator((a, b) -> a.contains(b) ? 0 : 1)
            .contains("null")
            .contains("negative");
    }

    @Test
    public void modelEffectsWorks() {
        final double powerLoadWatts = 10.0;
        final var powerState = new InstrumentPower();
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(final StateContainer _states) {
                SimulationEffects.spawnAndWait(new InstrumentOn<>(powerLoadWatts, powerState));
                assertThat(powerState.get()).isCloseTo( powerLoadWatts, withinPercentage(0.01));
            }
        };

        final var startTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        final var engine = new SimulationEngine(
            startTime,
            List.of(new ActivityJob<>(activity, startTime)),
            () -> List.of(powerState));
        engine.simulate();
    }
}
