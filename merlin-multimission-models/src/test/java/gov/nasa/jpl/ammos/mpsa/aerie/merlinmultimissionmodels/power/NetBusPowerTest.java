package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Collections;
import java.util.List;

/**
 * exercise basic functionality of the net bus power state
 */
public class NetBusPowerTest {

    /**
     * reusable mock state that just returns a fixed 100.0W
     */
    private final RandomAccessState<Double> mockState100_W = new MockState<>( 100.0 );

    /**
     * reusable mock state that just returns a fixed 50.0W
     */
    private final RandomAccessState<Double> mockState50_W = new MockState<>( 50.0 );

    /**
     * reusable mock state that just returns a fixed 350.0W
     */
    private final RandomAccessState<Double> mockState30_W = new MockState<>( 30.0 );


    @Test
    public void ctorWorks() {
        new NetBusPower( List.of(mockState100_W), List.of(mockState50_W) );
    }

    @Test
    public void ctorWorksWithEmptySources() {
        new NetBusPower( List.of(), List.of(mockState50_W) );
    }

    @Test
    public void ctorWorksWithEmptySinks() {
        new NetBusPower( List.of(mockState50_W), List.of() );
    }

    @Test
    public void ctorFailsOnNullSources() {
        final Throwable thrown = catchThrowable( ()->{
            new NetBusPower( null, List.of(mockState50_W) );
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining("null");
    }

    @Test
    public void ctorFailsOnNullSinks() {
        final Throwable thrown = catchThrowable( ()->{
            new NetBusPower( List.of(mockState50_W), null );
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining("null");
    }

    @Test
    public void ctorFailsOnNullSource() {
        final Throwable thrown = catchThrowable( ()->{
            new NetBusPower( Collections.singletonList( null ), List.of(mockState50_W) );
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining("null");
    }

    @Test
    public void ctorFailsOnNullSink() {
        final Throwable thrown = catchThrowable( ()->{
            new NetBusPower( List.of(mockState50_W), Collections.singletonList( null ) );
        });

        assertThat(thrown).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining("null");
    }

    @Test
    public void getWorks() {
        NetBusPower netState_W = new NetBusPower(
                List.of(mockState100_W), List.of(mockState50_W) );
        final double expected_W = 100.0 - 50.0;

        final double result_W = netState_W.get();

        assertThat( result_W ).isCloseTo( expected_W, withinPercentage( 0.01 ) );
    }

    @Test
    public void getWorksWithEmptySinks() {
        NetBusPower netState_W = new NetBusPower(
                List.of(mockState100_W), List.of() );
        final double expected_W = 100.0;

        final double result_W = netState_W.get();

        assertThat( result_W ).isCloseTo( expected_W, withinPercentage( 0.01 ) );
    }

    @Test
    public void getWorksWithEmptySources() {
        NetBusPower netState_W = new NetBusPower(
                List.of(), List.of(mockState50_W) );
        final double expected_W = -50.0;

        final double result_W = netState_W.get();

        assertThat( result_W ).isCloseTo( expected_W, withinPercentage( 0.01 ) );
    }

    @Test
    public void getWorksWithMultipleSourcesAndSinks() {
        NetBusPower netState_W = new NetBusPower(
                List.of(mockState100_W, mockState30_W),
                List.of(mockState50_W, mockState30_W) );
        final double expected_W = 100.0 + 30.0 - 50.0 - 30.0;

        final double result_W = netState_W.get();

        assertThat( result_W ).isCloseTo( expected_W, withinPercentage( 0.01 ) );
    }

}
