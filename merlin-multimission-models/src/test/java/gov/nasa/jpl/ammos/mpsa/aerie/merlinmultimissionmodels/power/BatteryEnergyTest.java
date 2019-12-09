package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockTimeSimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.Test;
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
    //TODO: would be grand if Time's valueOf returned an object instead of mutating one
    private final Time t2020 = new Time();
    { t2020.valueOf( "2020-001T00:00:00.000"); }
    private final Time t2020_10s = new Time();
    { t2020_10s.valueOf( "2020-001T00:00:10.000"); }
    private final Time t2020_20s = new Time();
    { t2020_20s.valueOf( "2020-001T00:00:20.000"); }

    /**
     * reusable simulation engine mocks
     */
    private final MockTimeSimulationEngine mockSimEngine2020 = new MockTimeSimulationEngine( t2020 );
    private final MockTimeSimulationEngine mockSimEngine2020_10s = new MockTimeSimulationEngine( t2020_10s );


    @Test
    public void ctorWorks() {
        new BatteryEnergy( 1110.0, t2020, mockState50_W, mockState10k_J );
    }

    @Test
    public void ctorFailsOnNullTime() {
        final Throwable thrown = catchThrowable( ()->{
            new BatteryEnergy( 1110.0, null, mockState50_W, mockState10k_J );
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "null" );
    }

    @Test
    public void ctorFailsOnNullPower() {
        final Throwable thrown = catchThrowable( ()->{
            new BatteryEnergy( 1110.0, t2020, null, mockState10k_J );
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "null" );
    }

    @Test
    public void ctorFailsOnNullMax() {
        final Throwable thrown = catchThrowable( ()->{
            new BatteryEnergy( 1110.0, t2020, mockState50_W, null );
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "null" );
    }

    @Test
    public void getWorksOnInitialValue() {
        final double initialCharge_J = 1110.0;
        final BatteryEnergy chargeState_J = new BatteryEnergy(
                initialCharge_J, t2020, mockState50_W, mockState10k_J );
        chargeState_J.setEngine( mockSimEngine2020 );
        final double expected_J = 1110.0;

        final double result_J = chargeState_J.get();

        assertThat( result_J ).isCloseTo( expected_J, withinPercentage( 0.01 ) );
    }

    @Test
    public void getFailsOnPast() {
        final double initialCharge_J = 1110.0;
        final BatteryEnergy chargeState_J = new BatteryEnergy(
                initialCharge_J, t2020_10s, mockState50_W, mockState10k_J );
        chargeState_J.setEngine( mockSimEngine2020 );

        final Throwable thrown = catchThrowable( ()-> {
            chargeState_J.get();
        });

        assertThat(thrown).isInstanceOf( AssertionError.class )
                .hasMessageContaining( "past" );
    }


    @Test
    public void getWorksOnIntegratingConstantFromZeroInitial() {
        final double initialCharge_J = 0.0;
        final BatteryEnergy chargeState_J = new BatteryEnergy(
                initialCharge_J, t2020, mockState50_W, mockState10k_J );
        chargeState_J.setEngine( mockSimEngine2020_10s );
        final double expected_J = 500.0; //0J + 10s of 50W = 500J

        final double result_J = chargeState_J.get();

        assertThat( result_J ).isCloseTo( expected_J, withinPercentage( 0.01 ) );
    }


    @Test
    public void getWorksOnDuplicateIntegrationQueries() {
        final double initialCharge_J = 0.0;
        BatteryEnergy chargeState_J = new BatteryEnergy(
                initialCharge_J, t2020, mockState50_W, mockState10k_J );
        chargeState_J.setEngine( mockSimEngine2020_10s );
        final double expected_J = 500.0; //0J + 10s of 50W = 500J

        final double result0_J = chargeState_J.get();
        final double result1_J = chargeState_J.get();

        assertThat( result0_J ).isCloseTo( expected_J, withinPercentage( 0.01 ) );
        assertThat( result1_J ).isCloseTo( expected_J, withinPercentage( 0.01 ) );
    }

    @Test
    public void getWorksOnSequentialIntegrationQueries() {
        final double initialCharge_J = 0.0;
        BatteryEnergy chargeState_J = new BatteryEnergy(
                initialCharge_J, t2020, mockState50_W, mockState10k_J );
        final MockTimeSimulationEngine mockSim = new MockTimeSimulationEngine( t2020 );
        chargeState_J.setEngine( mockSim );

        mockSim.setCurrentSimulationTime( t2020 );
        chargeState_J.get();

        mockSim.setCurrentSimulationTime( t2020_10s );
        chargeState_J.get();

        mockSim.setCurrentSimulationTime( t2020_20s );
        final double result20_J = chargeState_J.get();

        final double expected20_J = 1000.0; //0J + 10s of 50W + 10s of 50W = 1000J
        assertThat( result20_J ).isCloseTo( expected20_J, withinPercentage( 0.01 ) );
    }

    @Test
    public void getWorksOnIntegratingToMaxClamp() {
        final double initialCharge_J = 9900.0;
        final BatteryEnergy chargeState_J = new BatteryEnergy(
                initialCharge_J, t2020, mockState50_W, mockState10k_J );
        chargeState_J.setEngine( mockSimEngine2020_10s );
        final double expected_J = 10000.0; //9900J + 10s of 50W = 10400J, clamp to 10kJ

        final double result_J = chargeState_J.get();

        assertThat( result_J ).isCloseTo( expected_J, withinPercentage( 0.01 ) );
    }

}
