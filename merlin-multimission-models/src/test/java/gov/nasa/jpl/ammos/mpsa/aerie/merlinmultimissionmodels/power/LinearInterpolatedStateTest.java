package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.Test;


/**
 * exercises basic functionality and mathematics of the simple linear interpolation state
 */
public class LinearInterpolatedStateTest {

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

    @Test public void twoPointCtorWorks() {
        new LinearInterpolatedState( t2020, 0, t2020_10s, 10 );
    }

    @Test public void twoPointCtorFailsOnSameTime() {
        final Throwable thrown = catchThrowable( ()->{
            new LinearInterpolatedState( t2020, 0, t2020, 10 );
        });

        //make sure right exception thrown
        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "zero" );
    }


    @Test public void getAtLeftWorks() {
        LinearInterpolatedState state  = new LinearInterpolatedState(
                t2020, 100, t2020_10s, 110 );
        double result = state.get( t2020 );

        assertThat(result).isCloseTo( 100, withinPercentage( 0.01 ) );
    }

    @Test public void getAtRightWorks() {
        LinearInterpolatedState state  = new LinearInterpolatedState(
                t2020, 100, t2020_10s, 110 );
        double result = state.get( t2020_10s );

        assertThat(result).isCloseTo( 110, withinPercentage( 0.01 ) );
    }

    @Test public void getAtMiddleWorks() {
        LinearInterpolatedState state  = new LinearInterpolatedState(
                t2020, 100, t2020_20s, 120 );
        double result = state.get( t2020_10s );

        assertThat(result).isCloseTo( 110, withinPercentage( 0.01 ) );
    }

    @Test public void getPastLeftWorks() {
        LinearInterpolatedState state  = new LinearInterpolatedState(
                t2020_10s, 100, t2020_20s, 110 );
        double result = state.get( t2020 );

        assertThat(result).isCloseTo( 90, withinPercentage( 0.01 ) );
    }

    @Test public void getPastRightWorks() {
        LinearInterpolatedState state  = new LinearInterpolatedState(
                t2020, 100, t2020_10s, 110 );
        double result = state.get( t2020_20s );

        assertThat(result).isCloseTo( 120, withinPercentage( 0.01 ) );
    }

}
