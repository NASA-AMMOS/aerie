package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * tests various basic functionality of the battery percent charge state
 */
public class BatteryPercentChargeTest {

    /**
     * reusable mock state that just returns a fixed 200.0J
     */
    private final State<Double> mockState200_J = new MockState<>( 200.0 );

    /**
     * reusable mock state that just returns a fixed 50.0J
     */
    private final State<Double> mockState50_J = new MockState<>( 50.0 );

    /**
     * reusable mock state that just returns a fixed 0.0J
     */
    private final State<Double> mockState0_J = new MockState<>( 0.0 );

    @Test
    public void ctorWorks() {
        new BatteryPercentCharge(mockState50_J, mockState200_J);
    }

    @Test
    public void ctorFailsOnNullStored() {
        final Throwable thrown = catchThrowable( ()->{
            new BatteryPercentCharge( null, mockState200_J);
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "null" );
    }

    @Test public void ctorFailsOnNullCapacity() {
        final Throwable thrown = catchThrowable( ()->{
            new BatteryPercentCharge(mockState50_J, null );
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "null" );
    }

    @Test public void getWorks() {
        final BatteryPercentCharge chargeState_pct = new BatteryPercentCharge(
                mockState50_J, mockState200_J);
        final double expected_pct = 100.0 * ( 50.0 / 200.0 );

        final double result_pct = chargeState_pct.get();

        assertThat( result_pct ).isCloseTo( expected_pct, withinPercentage( 0.01 ) );
    }

    @Test public void getFailsOnZeroCapacity() {
        final BatteryPercentCharge chargeState_pct = new BatteryPercentCharge(
                mockState50_J, mockState0_J );
        final Throwable thrown = catchThrowable( ()->{
            chargeState_pct.get();
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "zero" );
    }

    @Test public void getFailsOnNegativeCapacity() {
        final BatteryPercentCharge chargeState_pct = new BatteryPercentCharge(
                mockState50_J, new MockState<>(-10.0) );
        final Throwable thrown = catchThrowable( ()->{
            chargeState_pct.get();
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "negative" );
    }

}
