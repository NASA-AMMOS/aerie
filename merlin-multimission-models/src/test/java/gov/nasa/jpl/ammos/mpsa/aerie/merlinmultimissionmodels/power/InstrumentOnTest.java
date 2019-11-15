package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockEmptyStateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockSimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockTimeEmptySimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * unit tests of the multi-mission instrument power on activity
 *
 * these unit tests do not run the actual merlin simulation engine
 */
public class InstrumentOnTest {

    /**
     * reusable mock of state container with no states
     */
    private final MockEmptyStateContainer mockStates = new MockEmptyStateContainer();

    /**
     * reusable mock of a non-functional simulation context that throws on any calls
     */
    private final MockSimulationContext mockContext = new MockSimulationContext();


    @Test public void defaultCtorWorks() {
        new InstrumentOn();
    }

    @Test public void argCtorWorks() {
        final InstrumentPower powerState_W = new InstrumentPower();
        new InstrumentOn<MockEmptyStateContainer>( 100.0, powerState_W );
    }

    @Test public void validationWorks() {
        final InstrumentPower powerState_W = new InstrumentPower();
        final InstrumentOn<MockEmptyStateContainer> onAct =
                new InstrumentOn<>( 100.0, powerState_W );

        final List<String> errors = onAct.validateParameters();

        assertThat(errors).hasSize( 0 );
    }

    @Test public void validationFailsOnNegativePower() {
        final InstrumentPower powerState_W = new InstrumentPower();
        final InstrumentOn<MockEmptyStateContainer> onAct =
                new InstrumentOn<>( -5.0, powerState_W );

        final List<String> errors = onAct.validateParameters();

        assertThat(errors).hasSize( 1 )
                .usingElementComparator((a,b)->a.contains(b)?0:1).contains("negative");
    }

    @Test public void validationFailsOnNullState() {
        final InstrumentOn<MockEmptyStateContainer> onAct =
                new InstrumentOn<>( 100.0, null );

        final List<String> errors = onAct.validateParameters();

        assertThat(errors).hasSize( 1 )
                .usingElementComparator((a,b)->a.contains(b)?0:1).contains("null");
    }

    @Test public void validationFailsOnBothNegativePowerAndNullState() {
        final InstrumentOn<MockEmptyStateContainer> onAct =
                new InstrumentOn<>( -5.0, null );

        List<String> errors = onAct.validateParameters();

        assertThat(errors).hasSize( 2 ).usingElementComparator((a,b)-> a.contains(b)?0:1)
                .contains("null").contains("negative");
    }

    @Test public void modelEffectsWorks() {
        final double powerLoad_W = 10.0;
        final InstrumentPower powerState_W = new InstrumentPower();
        powerState_W.setEngine( new MockTimeEmptySimulationEngine( new Time() ) );

        //TODO: use a mock state class, except right now it would be identical!
        final InstrumentOn<MockEmptyStateContainer> onAct =
                new InstrumentOn<>( powerLoad_W, powerState_W );

        onAct.modelEffects( mockContext, mockStates );

        final double resultPower_W = powerState_W.get();
        assertThat(resultPower_W).isCloseTo( powerLoad_W, withinPercentage(0.01));
    }



}
