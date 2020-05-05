package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.junit.Test;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.withEffects;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * exercises the basic (non-random access) functionality of the instrument power state
 */
public class InstrumentPowerTest {
    @Test
    public void defaultCtorWorks() {
        //configure the instrument power model
        new InstrumentPower();

        //no exception!
    }

    @Test
    public void getStartsAtZeroPower() {
        final var simEngine = new SimulationEngine();

        //configure the instrument power model
        final var state = new InstrumentPower();
        state.initialize(simEngine.getCurrentTime());

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            assertThat(state.get()).isCloseTo(0.0, withinPercentage(0.01));
        }));

        simEngine.runToCompletion();
    }

    @Test
    public void setWorks() {
        final var simEngine = new SimulationEngine();

        //configure the instrument power model (it starts at zero)
        final var state = new InstrumentPower();
        state.initialize(simEngine.getCurrentTime());

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            state.set(330.0);
        }));

        simEngine.runToCompletion();
    }

    @Test
    public void getWorksAfterSet() {
        final var simEngine = new SimulationEngine();

        //configure the instrument power model (it starts at zero)
        final var state = new InstrumentPower();
        state.initialize(simEngine.getCurrentTime());

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            //set the power to some test value
            final double testValue = 330.0;
            state.set(testValue);

            //ensure the same value is returned
            assertThat(state.get()).isCloseTo(testValue, withinPercentage(0.01));
        }));

        simEngine.runToCompletion();
    }
}
