package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.Test;

/**
 * exercises basic functionality of the SolarArrayPower state
 */
public class SolarArrayPowerTest {

    /**
     * the reference distance used by the mock distance state
     */
    private final double meters_per_astronomical_unit = 1.0e+11;

    /**
     * reusable mock state that just returns the referenceDistance_m
     */
    private final RandomAccessState<Double> oneAUMockState_m
            = new MockState<>( meters_per_astronomical_unit );


    @Test
    public void defaultConfigCtorWorks() {
        //use the ctor that uses the default configuration
        final SolarArrayPower testPowerState_W = new SolarArrayPower( oneAUMockState_m );

        //no exception!
    }

    @Test
    public void defaultConfigCtorFailsOnNullDistanceState() {
        //try to run the ctor with null
        final Throwable thrown = catchThrowable( ()->{
            new SolarArrayPower( null );
        });

        //make sure right exception thrown
        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "null" );
    }


    @Test
    public void configuringCtorWorks() {
        //configure the solar power model with some test values
        final double refPower_W = 100.0;
        final SolarArrayPower testPowerState_W = new SolarArrayPower(
                oneAUMockState_m,
                refPower_W,
                meters_per_astronomical_unit
        );

        //no exception!
    }

    @Test
    public void configurationCtorFailsOnNullDistanceState() {
        //try to run the ctor with null
        final Throwable thrown = catchThrowable( ()->{
            new SolarArrayPower( null, 100.0, 1.0e11 );
        });

        //make sure right exception thrown
        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "null" );
    }

    @Test
    public void configurationCtorFailsOnZeroReferencDistance() {
        //try to run the ctor with zero dist
        final Throwable thrown = catchThrowable( ()->{
            new SolarArrayPower( oneAUMockState_m, 100.0, 0.0 );
        });

        //make sure right exception thrown
        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "zero" );
    }

    @Test
    public void configurationCtorFailsOnNegativeReferencDistance() {
        //try to run the ctor with negative dist
        final Throwable thrown = catchThrowable( ()->{
            new SolarArrayPower( oneAUMockState_m, 100.0, -1.0e11 );
        });

        //make sure right exception thrown
        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "negative" );
    }

    @Test
    public void configurationCtorWorksOnZeroReferencPower() {
        //try to run the ctor with zero power
        new SolarArrayPower( oneAUMockState_m, 0.0, 1.0e11 );

        //no exception expected!
    }

    @Test
    public void configurationCtorFailsOnNegativeReferencPower() {
        //try to run the ctor with negative power
        final Throwable thrown = catchThrowable( ()->{
            new SolarArrayPower( oneAUMockState_m, -100.0, 1.0e11 );
        });

        //make sure right exception thrown
        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "negative" );
    }


    @Test
    public void getOnDefaultConfigWorks() {
        //use the ctor that uses the default configuration
        final SolarArrayPower testPowerState_W = new SolarArrayPower( oneAUMockState_m );

        //run the model
        final double resultPower_W = testPowerState_W.get();

        //just make sure the value is sensical
        Assertions.assertThat( resultPower_W ).isNotNegative();
    }

    @Test
    public void getOnConfiguredWorks() {
        //configure the solar power model with some test values
        final double refPower_W = 100.0;
        final SolarArrayPower testPowerState_W = new SolarArrayPower(
                oneAUMockState_m,
                refPower_W,
                meters_per_astronomical_unit
        );

        //run the model
        final double resultPower_W = testPowerState_W.get();

        //make sure we get the expected power output
        assertThat( resultPower_W ).isCloseTo( refPower_W, withinPercentage( 0.01 ) );
    }

    @Test
    public void getFailsOnZeroSolarDistance() {
        //setup power state to get a zero solar distance
        final RandomAccessState<Double> zeroMockState_m = new MockState<>( 0.0 );
        final State<Double> powerState_W = new SolarArrayPower( zeroMockState_m );

        //call the model
        final Throwable thrown = catchThrowable( ()->{ powerState_W.get(); });

        //make sure right exception thrown
        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining("zero");
    }

    @Test
    public void getFailsOnNegativeSolarDistance() {
        //setup power state to get a negative solar distance
        final RandomAccessState<Double> negativeMockState_m = new MockState<>( -1.0e11 );
        final State<Double> powerState_W = new SolarArrayPower( negativeMockState_m );

        //call the model
        final Throwable thrown = catchThrowable( ()->{ powerState_W.get(); });

        //make sure right exception thrown
        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining("negative");
    }

}
