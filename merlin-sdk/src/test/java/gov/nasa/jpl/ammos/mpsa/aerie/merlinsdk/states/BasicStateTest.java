package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class BasicStateTest {

    //we can always add more tests for each type

    public String name1 = "State 1";
    public String name2 = "State 2";
    public BasicState<Integer> integerBasicState = new BasicState<>(name1, 0);
    public BasicState<String> stringBasicState = new BasicState<>(name2, "");
    public int val1 = 12;
    public String val2 = "NADIR";

    public class MockStateContainer implements StateContainer {
        public List<State<?>> getStateList() {
            return List.of();
        }
    }
    public SimulationEngine mockEngine = new SimulationEngine(
        SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS),
        List.of(),
        new MockStateContainer());


    @Before
    public void setup() {
        integerBasicState.setEngine(mockEngine);
        stringBasicState.setEngine(mockEngine);
        integerBasicState.set(val1);
        stringBasicState.set(val2);
    }

    @After
    public void teardown(){
        integerBasicState = null;
        stringBasicState = null;
    }

    @Test
    public void getValue(){
        assert(integerBasicState.get() == val1);
        assert(stringBasicState.get() == val2);
    }
}

