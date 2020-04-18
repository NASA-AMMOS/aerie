package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import org.junit.Test;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.spawn;
import static org.assertj.core.api.Assertions.assertThat;

public class BasicStateTest {
    @Test
    public void integerState() {
        final var startTime = SimulationInstant.ORIGIN;

        final var state = new BasicState<>("State 1", 0);
        state.initialize(startTime);

        final var activity = new Activity() {
            @Override
            public void modelEffects() {
                state.set(12);
                assertThat(state.get()).isEqualTo(12);
            }
        };

        SimulationEngine.simulate(startTime, () -> spawn(activity));
    }

    @Test
    public void stringState() {
        final var startTime = SimulationInstant.ORIGIN;

        final var state = new BasicState<>("State 2", "");
        state.initialize(startTime);

        final var activity = new Activity() {
            @Override
            public void modelEffects() {
                state.set("NADIR");
                assertThat(state.get()).isEqualTo("NADIR");
            }
        };

        SimulationEngine.simulate(startTime, () -> spawn(activity));
    }
}

