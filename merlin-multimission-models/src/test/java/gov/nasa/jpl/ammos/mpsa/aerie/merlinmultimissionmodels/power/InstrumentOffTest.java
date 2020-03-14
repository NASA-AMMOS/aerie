package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import org.junit.Test;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.spawn;
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
        final var startTime = SimulationInstant.ORIGIN;

        SimulationEngine.simulate(
            startTime,
            () -> List.of(powerState),
            () -> {
                powerState.set(10.0);
                spawn(new InstrumentOff<>(powerState)).await();

                assertThat(powerState.get()).isCloseTo( 0.0, withinPercentage(0.01));
            });
    }
}
