package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * test basic functionality of the battery energy state in isolation
 */
public class BatteryEnergyTest {

    /**
     * reusable mock state that just returns a fixed 10kJ
     */
    private final RandomAccessState<Double> mockState10k_J = new MockState<>( 10000.0 );

    /**
     * reusable mock state that just returns a fixed 50.0W
     */
    private final RandomAccessState<Double> mockState50_W = new MockState<>( 50.0 );

    /**
     * reusable time points
     */
    private final Instant t2020 = SimulationInstant.fromQuantity(0, TimeUnit.SECONDS);

    @Test
    public void ctorWorks() {
        new BatteryEnergy( 1110.0, mockState50_W, mockState10k_J );
    }

    @Test
    public void ctorFailsOnNullPower() {
        final var thrown = catchThrowable(() -> new BatteryEnergy(1110.0, null, mockState10k_J));

        assertThat(thrown)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("null");
    }

    @Test
    public void ctorFailsOnNullMax() {
        final var thrown = catchThrowable(() -> new BatteryEnergy(1110.0, mockState50_W, null));

        assertThat(thrown)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("null");
    }

    @Test
    public void getWorksOnInitialValue() {
        final double initialCharge_J = 1110.0;
        final double expected_J = 1110.0;

        final var chargeState_J = new BatteryEnergy(initialCharge_J, mockState50_W, mockState10k_J);
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(StateContainer states) {
                assertThat(chargeState_J.get()).isCloseTo(expected_J, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            t2020,
            List.of(new ActivityJob<>(activity, t2020)),
            () -> List.of(chargeState_J));
    }


    @Test
    public void getWorksOnIntegratingConstantFromZeroInitial() {
        final double initialCharge_J = 0.0;
        final double expected_J = 500.0; //0J + 10s of 50W = 500J

        final var chargeState_J = new BatteryEnergy(initialCharge_J, mockState50_W, mockState10k_J);
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(StateContainer states) {
                SimulationEffects.delay(10, TimeUnit.SECONDS);
                assertThat(chargeState_J.get()).isCloseTo(expected_J, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            t2020,
            List.of(new ActivityJob<>(activity, t2020)),
            () -> List.of(chargeState_J));
    }


    @Test
    public void getWorksOnDuplicateIntegrationQueries() {
        final double initialCharge_J = 0.0;
        final double expected_J = 500.0; //0J + 10s of 50W = 500J

        final var chargeState_J = new BatteryEnergy( initialCharge_J, mockState50_W, mockState10k_J );
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(StateContainer states) {
                SimulationEffects.delay(10, TimeUnit.SECONDS);
                assertThat(chargeState_J.get()).isCloseTo(expected_J, withinPercentage(0.01));
                assertThat(chargeState_J.get()).isCloseTo(expected_J, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            t2020,
            List.of(new ActivityJob<>(activity, t2020)),
            () -> List.of(chargeState_J));
    }

    @Test
    public void getWorksOnSequentialIntegrationQueries() {
        final double initialCharge_J = 0.0;
        final double expected20_J = 1000.0; //0J + 10s of 50W + 10s of 50W = 1000J

        final var chargeState_J = new BatteryEnergy(initialCharge_J, mockState50_W, mockState10k_J);
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(StateContainer states) {
                chargeState_J.get();
                SimulationEffects.delay(10, TimeUnit.SECONDS);
                chargeState_J.get();
                SimulationEffects.delay(10, TimeUnit.SECONDS);
                assertThat(chargeState_J.get()).isCloseTo(expected20_J, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            t2020,
            List.of(new ActivityJob<>(activity, t2020)),
            () -> List.of(chargeState_J));
    }

    @Test
    public void getWorksOnIntegratingToMaxClamp() {
        final double initialCharge_J = 9900.0;
        final double expected_J = 10000.0; //9900J + 10s of 50W = 10400J, clamp to 10kJ

        final var chargeState_J = new BatteryEnergy(initialCharge_J, mockState50_W, mockState10k_J);
        final var activity = new Activity<>() {
            @Override
            public void modelEffects(StateContainer states) {
                SimulationEffects.delay(10, TimeUnit.SECONDS);
                assertThat(chargeState_J.get()).isCloseTo(expected_J, withinPercentage(0.01));
            }
        };

        SimulationEngine.simulate(
            t2020,
            List.of(new ActivityJob<>(activity, t2020)),
            () -> List.of(chargeState_J));
    }
}
