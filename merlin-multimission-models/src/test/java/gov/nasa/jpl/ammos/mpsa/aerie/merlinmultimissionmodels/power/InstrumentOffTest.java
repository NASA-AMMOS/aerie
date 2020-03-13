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
 * unit tests of the multi-mission instrument power off activity
 *
 * these unit tests do not run the actual merlin simulation engine
 */
public class InstrumentOffTest {
    @Test
    public void defaultCtorWorks() {
        new InstrumentOff<>();
    }

    @Test
    public void argCtorWorks() {
        final var state = new InstrumentPower();
        new InstrumentOff<>(state);
    }

    @Test
    public void validationWorks() {
        final var state = new InstrumentPower();
        final var activity = new InstrumentOff<>(state);

        assertThat(activity.validateParameters()).isEmpty();
    }

    @Test
    public void validationFailsOnNullState() {
        final var activity = new InstrumentOff<>(null);

        assertThat(activity.validateParameters())
            .hasSize(1)
            .usingElementComparator((a, b) -> a.contains(b) ? 0 : 1)
            .contains("null");
    }

    @Test
    public void modelEffectsWorks() {
        final var powerState = new InstrumentPower();
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(final StateContainer _states) {
                powerState.set(10.0);
                SimulationEffects.spawnAndWait(new InstrumentOff<>(powerState));

                assertThat(powerState.get()).isCloseTo( 0.0, withinPercentage(0.01));
            }
        };

        final var startTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        SimulationEngine.simulate(
            startTime,
            List.of(new ActivityJob<>(activity, startTime)),
            () -> List.of(powerState));
    }
}
