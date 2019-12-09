package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;


import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockSimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockTimeSimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * exercises the random-access functionality of the instrument power state
 */
public class InstrumentPowerRandomAccessTest {

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
    private final MockTimeSimulationEngine mockSimEngine2020_20s = new MockTimeSimulationEngine( t2020_20s );


    @Test
    public void getAtTimeStartsAtZeroPower() {
        final InstrumentPower instPower_W = new InstrumentPower();
        instPower_W.setEngine( mockSimEngine2020 );

        final double resultValue_W = instPower_W.get( t2020 );

        assertThat( resultValue_W ).isCloseTo( 0.0, withinPercentage( 0.01 ) );
    }

    @Test
    public void getAtTimeSeesContemporarySet() {
        final InstrumentPower instPower_W = new InstrumentPower();
        instPower_W.setEngine( mockSimEngine2020 );
        final double testValue_W = 300.0;

        instPower_W.set( testValue_W );
        final double resultValue_W = instPower_W.get( t2020 );

        assertThat( resultValue_W ).isCloseTo( testValue_W, withinPercentage( 0.01 ) );
    }

    @Test
    public void getAtTimeSeesPastSet() {
        final InstrumentPower instPower_W = new InstrumentPower();

        final double testValue_W = 300.0;
        instPower_W.setEngine( mockSimEngine2020 );
        instPower_W.set( testValue_W );

        instPower_W.setEngine( mockSimEngine2020_10s );
        final double resultValue_W = instPower_W.get( t2020 );

        assertThat( resultValue_W ).isCloseTo( testValue_W, withinPercentage( 0.01 ) );
    }

    @Test
    public void getAtTimeSeesPastSetDespiteNewSet() {
        final InstrumentPower instPower_W = new InstrumentPower();

        final double testValue_W = 300.0;
        instPower_W.setEngine( mockSimEngine2020 );
        instPower_W.set( testValue_W );

        instPower_W.setEngine( mockSimEngine2020_10s );
        instPower_W.set( 888.0 );

        final double resultValue_W = instPower_W.get( t2020 );

        assertThat( resultValue_W ).isCloseTo( testValue_W, withinPercentage( 0.01 ) );
    }

    @Test
    public void getAtTimeSeesIntermediatePastSet() {
        final InstrumentPower instPower_W = new InstrumentPower();

        final double testValue_W = 300.0;
        instPower_W.setEngine( mockSimEngine2020 );
        instPower_W.set( testValue_W );

        instPower_W.setEngine( mockSimEngine2020_20s );
        instPower_W.set( 888.0 );

        final double resultValue_W = instPower_W.get( t2020_10s );

        assertThat( resultValue_W ).isCloseTo( testValue_W, withinPercentage( 0.01 ) );
    }

    @Test
    public void getAtTimeSeesSeriesOfPastSets() {
        final InstrumentPower instPower_W = new InstrumentPower();
        final Map<String,Double> sets = new TreeMap<>( Map.of(
                "2020-001T00:00:10.000", 110.0,
                "2020-001T00:00:20.000", 120.0,
                "2020-001T00:00:30.000", 130.0 ) );
        final Map<String,Double> expecteds =  Map.of(
                "2020-001T00:00:00.000", 0.0,
                "2020-001T00:00:10.000", 110.0,
                "2020-001T00:00:15.000", 110.0,
                "2020-001T00:00:20.000", 120.0,
                "2020-001T00:00:29.000", 120.0,
                "2020-001T00:00:30.000", 130.0,
                "2020-001T00:00:31.000", 130.0 );
        final Time finalTime = new Time();
        finalTime.valueOf( "2020-001T00:01:00.000" );
        final MockTimeSimulationEngine finalEngine = new MockTimeSimulationEngine( finalTime );

        //run all sets in test vector sequentially (thanks to TreeMap being sorted)
        for( Map.Entry<String,Double> set : sets.entrySet() ) {
            final Time setT = new Time(); setT.valueOf( set.getKey() );
            final MockTimeSimulationEngine engineT = new MockTimeSimulationEngine( setT );

            instPower_W.setEngine( engineT );
            instPower_W.set( set.getValue() );
        }

        //jump past end of simulation steps
        instPower_W.setEngine( finalEngine );

        //now test each expected point in random access pattern (thanks HashMap...)
        for( Map.Entry<String,Double> expected : expecteds.entrySet() ) {
            final Time expectedT = new Time(); expectedT.valueOf( expected.getKey() );

            final double resultValue_W = instPower_W.get( expectedT );

            assertThat( resultValue_W ).isCloseTo( expected.getValue(), withinPercentage( 0.01 ) );
        }
    }

}
