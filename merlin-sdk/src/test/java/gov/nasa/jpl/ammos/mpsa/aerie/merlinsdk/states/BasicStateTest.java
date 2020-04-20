package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.junit.Test;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.withEffects;
import static org.assertj.core.api.Assertions.assertThat;

public class BasicStateTest {
    @Test
    public void integerState() {
        final var simEngine = new SimulationEngine();

        final var state = new BasicState<>("State 1", 0);
        state.initialize(simEngine.getCurrentTime());

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            state.set(12);
            assertThat(state.get()).isEqualTo(12);
        }));

        simEngine.runToCompletion();
    }

    @Test
    public void stringState() {
        final var simEngine = new SimulationEngine();

        final var state = new BasicState<>("State 2", "");
        state.initialize(simEngine.getCurrentTime());

        simEngine.scheduleJobAfter(Duration.ZERO, withEffects(() -> {
            state.set("NADIR");
            assertThat(state.get()).isEqualTo("NADIR");
        }));

        simEngine.runToCompletion();
    }
}

