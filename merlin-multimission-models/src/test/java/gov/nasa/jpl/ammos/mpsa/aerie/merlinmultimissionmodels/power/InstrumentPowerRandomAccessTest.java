package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockTimeSimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * exercises the random-access functionality of the instrument power state
 */
public class InstrumentPowerRandomAccessTest {

    /**
     * reusable time points
     */
    private final Instant t2020 = SimulationInstant.origin();
    private final Instant t2020_10s = t2020.plus(10, TimeUnit.SECONDS);
    private final Instant t2020_20s = t2020.plus(20, TimeUnit.SECONDS);

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
        final Instant initialInstant = SimulationInstant.origin();
        final InstrumentPower instPower_W = new InstrumentPower();
        final Map<Instant,Double> sets = new TreeMap<>( Map.of(
                initialInstant.plus(10, TimeUnit.SECONDS), 110.0,
                initialInstant.plus(20, TimeUnit.SECONDS), 120.0,
                initialInstant.plus(30, TimeUnit.SECONDS), 130.0 ) );
        final Map<Instant,Double> expecteds =  Map.of(
                initialInstant.plus(0, TimeUnit.SECONDS),    0.0,
                initialInstant.plus(10, TimeUnit.SECONDS), 110.0,
                initialInstant.plus(15, TimeUnit.SECONDS), 110.0,
                initialInstant.plus(20, TimeUnit.SECONDS), 120.0,
                initialInstant.plus(25, TimeUnit.SECONDS), 120.0,
                initialInstant.plus(30, TimeUnit.SECONDS), 130.0,
                initialInstant.plus(31, TimeUnit.SECONDS), 130.0 );
        final Instant finalTime = initialInstant.plus(1, TimeUnit.MINUTES);
        final MockTimeSimulationEngine engine = new MockTimeSimulationEngine( initialInstant );

        instPower_W.setEngine( engine );

        //run all sets in test vector sequentially (thanks to TreeMap being sorted)
        for( Map.Entry<Instant,Double> set : sets.entrySet() ) {
            engine.setCurrentSimulationTime(set.getKey());
            instPower_W.set( set.getValue() );
        }

        //jump past end of simulation steps
        engine.setCurrentSimulationTime(finalTime);

        //now test each expected point in random access pattern (thanks HashMap...)
        for( Map.Entry<Instant,Double> expected : expecteds.entrySet() ) {
            final double resultValue_W = instPower_W.get( expected.getKey() );

            assertThat( resultValue_W ).isCloseTo( expected.getValue(), withinPercentage( 0.01 ) );
        }
    }

}
