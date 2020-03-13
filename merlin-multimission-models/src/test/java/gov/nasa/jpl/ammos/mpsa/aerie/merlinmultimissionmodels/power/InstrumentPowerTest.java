package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * exercises the basic (non-random access) functionality of the instrument power state
 */
public class InstrumentPowerTest {
    private static final Instant simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);

    @Test
    public void defaultCtorWorks() {
        //configure the instrument power model
        new InstrumentPower();

        //no exception!
    }

    @Test
    public void getStartsAtZeroPower() {
        //configure the instrument power model
        final SettableState<Double> state = new InstrumentPower();
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(final StateContainer _states) {
                assertThat(state.get()).isCloseTo(0.0, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            simStart,
            List.of(Pair.of(simStart, activity)),
            () -> List.of(state));
    }

    @Test
    public void setWorks() {
        //configure the instrument power model (it starts at zero)
        final SettableState<Double> state = new InstrumentPower();
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(final StateContainer _states) {
                //set the power to some test value
                state.set(330.0);
            }
        };

        SimulationEngine.simulate(
            simStart,
            List.of(Pair.of(simStart, activity)),
            () -> List.of(state));
    }

    @Test
    public void getWorksAfterSet() {
        //configure the instrument power model (it starts at zero)
        final SettableState<Double> state = new InstrumentPower();
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(final StateContainer _states) {
                //set the power to some test value
                final double testValue = 330.0;
                state.set(testValue);

                //ensure the same value is returned
                assertThat(state.get()).isCloseTo(testValue, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            simStart,
            List.of(Pair.of(simStart, activity)),
            () -> List.of(state));
    }
}
