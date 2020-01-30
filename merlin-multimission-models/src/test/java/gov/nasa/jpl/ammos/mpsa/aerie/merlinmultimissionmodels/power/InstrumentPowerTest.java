package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockTimeEmptySimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * exercises the basic (non-random access) functionality of the instrument power state
 */
public class InstrumentPowerTest {

    @Test
    public void defaultCtorWorks() {
        //configure the instrument power model
        SettableState<Double> instPower_W = new InstrumentPower();

        //no exception!
    }

    @Test
    public void getStartsAtZeroPower() {
        //configure the instrument power model
        SettableState<Double> instPower_W = new InstrumentPower();

        //get the initial, unperturbed value of the state
        final double resultValue_W = instPower_W.get();

        //ensure the initial value is off (ie 0.0)
        assertThat( resultValue_W ).isCloseTo( 0.0, withinPercentage( 0.01 ) );
    }

    @Test
    public void setWorks() {
        //configure the instrument power model (it starts at zero)
        SettableState<Double> instPower_W = new InstrumentPower();
        instPower_W.setEngine( new MockTimeEmptySimulationEngine( SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS) ) );

        //set the power to some test value
        final double testValue_W = 330.0;
        instPower_W.set( testValue_W );
    }

    @Test
    public void getWorksAfterSet() {
        //configure the instrument power model (it starts at zero)
        SettableState<Double> instPower_W = new InstrumentPower();
        instPower_W.setEngine( new MockTimeEmptySimulationEngine( SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS) ) );

        //set the power to some test value
        final double testValue_W = 330.0;
        instPower_W.set( testValue_W );

        //then fetch the changed value
        final double resultValue_W = instPower_W.get();

        //ensure the same value is returned
        assertThat( resultValue_W ).isCloseTo( testValue_W, withinPercentage( 0.01 ) );
    }

}
