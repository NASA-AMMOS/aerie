package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * exercises the basic (non-random access) functionality of the instrument power state
 */
public class InstrumentPowerTest {
    private static final Instant simStart = SimulationInstant.ORIGIN;

    @Test
    public void defaultCtorWorks() {
        //configure the instrument power model
        new InstrumentPower();

        //no exception!
    }

    @Test
    public void getStartsAtZeroPower() {
        //configure the instrument power model
        final var state = new InstrumentPower();

        SimulationEngine.simulate(simStart, List.of(state), () -> {
            assertThat(state.get()).isCloseTo(0.0, withinPercentage(0.01));
        });
    }

    @Test
    public void setWorks() {
        //configure the instrument power model (it starts at zero)
        final var state = new InstrumentPower();

        SimulationEngine.simulate(simStart, List.of(state), () -> {
            state.set(330.0);
        });
    }

    @Test
    public void getWorksAfterSet() {
        //configure the instrument power model (it starts at zero)
        final var state = new InstrumentPower();

        SimulationEngine.simulate(simStart, List.of(state), () -> {
            //set the power to some test value
            final double testValue = 330.0;
            state.set(testValue);

            //ensure the same value is returned
            assertThat(state.get()).isCloseTo(testValue, withinPercentage(0.01));
        });
    }
}
