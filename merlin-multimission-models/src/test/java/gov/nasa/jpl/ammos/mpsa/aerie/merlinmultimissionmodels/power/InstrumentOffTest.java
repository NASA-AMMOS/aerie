package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockEmptyStateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockSimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockTimeEmptySimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * unit tests of the multi-mission instrument power off activity
 *
 * these unit tests do not run the actual merlin simulation engine
 */
public class InstrumentOffTest {

    /**
     * reusable mock of state container with no states
     */
    private final MockEmptyStateContainer mockStates = new MockEmptyStateContainer();

    /**
     * reusable mock of a non-functional simulation context that throws on any calls
     */
    private final MockSimulationContext mockContext = new MockSimulationContext();


    @Test public void defaultCtorWorks() {
        new InstrumentOff();
    }

    @Test public void argCtorWorks() {
        final InstrumentPower powerState_W = new InstrumentPower();
        new InstrumentOff<MockEmptyStateContainer>( powerState_W );
    }

    @Test public void validationWorks() {
        final InstrumentPower powerState_W = new InstrumentPower();
        final InstrumentOff<MockEmptyStateContainer> onAct =
                new InstrumentOff<>( powerState_W );

        final List<String> errors = onAct.validateParameters();

        assertThat(errors).hasSize( 0 );
    }

    @Test public void validationFailsOnNullState() {
        final InstrumentOff<MockEmptyStateContainer> onAct =
                new InstrumentOff<>( null );

        final List<String> errors = onAct.validateParameters();

        assertThat(errors).hasSize( 1 )
                .usingElementComparator((a,b)->a.contains(b)?0:1).contains("null");
    }


    @Test public void modelEffectsWorks() {
        final double powerLoad_W = 10.0;
        final InstrumentPower powerState_W = new InstrumentPower();
        powerState_W.setEngine( new MockTimeEmptySimulationEngine(SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS)) );
        //TODO: use a mock state class, except right now it would be identical!

        final InstrumentOff<MockEmptyStateContainer> onAct =
                new InstrumentOff<>( powerState_W );

        powerState_W.set( powerLoad_W );
        onAct.modelEffects( mockContext, mockStates );

        final double resultPower_W = powerState_W.get();
        assertThat(resultPower_W).isCloseTo( 0.0, withinPercentage(0.01));
    }



}
