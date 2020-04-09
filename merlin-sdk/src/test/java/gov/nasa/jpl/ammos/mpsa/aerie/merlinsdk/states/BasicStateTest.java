package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicStateTest {
    @Test
    public void integerState() {
        final var state = new BasicState<>("State 1", 0);

        final var activity = new Activity<>() {
            @Override
            public void modelEffects() {
                state.set(12);
                assertThat(state.get()).isEqualTo(12);
            }
        };

        final var startTime = SimulationInstant.ORIGIN;
        SimulationEngine.simulate(
            startTime,
            () -> List.of(state),
            () -> SimulationEffects.spawn(activity));
    }

    @Test
    public void stringState() {
        final var state = new BasicState<>("State 2", "");

        final var activity = new Activity<>() {
            @Override
            public void modelEffects() {
                state.set("NADIR");
                assertThat(state.get()).isEqualTo("NADIR");
            }
        };

        final var startTime = SimulationInstant.ORIGIN;
        SimulationEngine.simulate(
            startTime,
            () -> List.of(state),
            () -> SimulationEffects.spawn(activity));
    }
}

