package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.assertj.core.api.Assertions.catchThrowable;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockTimeSimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import java.util.List;


/**
 * exercises basic functionality and mathematics of the simple linear interpolation state
 */
public class LinearInterpolatedStateTest {

    public class MockStateContainer implements StateContainer {
        public List<State<?>> getStateList() {
            return List.of();
        }
    }

    private final SimulationEngine mockEngine = new SimulationEngine(
        SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS),
        List.of(),
        new MockStateContainer());

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

    private final Instant simStart = SimulationInstant.fromQuantity(0, TimeUnit.SECONDS);
    private final Instant simStart_10s = simStart.plus(10, TimeUnit.SECONDS);
    private final Instant simStart_20s = simStart.plus(20, TimeUnit.SECONDS);

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
        state.setEngine(mockEngine);
        double result = state.get( simStart );

        assertThat(result).isCloseTo( 100, withinPercentage( 0.01 ) );
    }

    @Test public void getAtRightWorks() {
        LinearInterpolatedState state  = new LinearInterpolatedState(
                t2020, 100, t2020_10s, 110 );
        state.setEngine(mockEngine);
        double result = state.get( simStart_10s );

        assertThat(result).isCloseTo( 110, withinPercentage( 0.01 ) );
    }

    @Test public void getAtMiddleWorks() {
        LinearInterpolatedState state  = new LinearInterpolatedState(
                t2020, 100, t2020_20s, 120 );
        state.setEngine(mockEngine);
        double result = state.get( simStart_10s );

        assertThat(result).isCloseTo( 110, withinPercentage( 0.01 ) );
    }

    @Test public void getPastLeftWorks() {
        final MockTimeSimulationEngine<?> mockEngine = new MockTimeSimulationEngine<>(
            SimulationInstant.fromQuantity(10, TimeUnit.SECONDS) );

        LinearInterpolatedState state  = new LinearInterpolatedState(
                t2020_10s, 100, t2020_20s, 110 );
        state.setEngine(mockEngine);

        mockEngine.setCurrentSimulationTime(SimulationInstant.fromQuantity(0, TimeUnit.SECONDS));
        double result = state.get( simStart );

        assertThat(result).isCloseTo( 90, withinPercentage( 0.01 ) );
    }

    @Test public void getPastRightWorks() {
        LinearInterpolatedState state  = new LinearInterpolatedState(
                t2020, 100, t2020_10s, 110 );
        state.setEngine(mockEngine);
        double result = state.get( simStart_20s );

        assertThat(result).isCloseTo( 120, withinPercentage( 0.01 ) );
    }

}
