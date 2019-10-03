package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class SettableStatesTest {

    //we can always add more tests for each type

    public String name1 = "State 1";
    public String name2 = "State 2";
    public SettableState<Integer> integerSettableState = new SettableState<>(name1, 0);
    public SettableState<String> stringSettableState = new SettableState<>(name2, "");
    public int val1 = 12;
    public String val2 = "NADIR";

    public class MockStateContainer implements StateContainer {
        public List<State<?>> getStateList() {
            return List.of();
        }
    }
    public SimulationEngine<?> mockEngine = new SimulationEngine<StateContainer>(new Time(), List.of(), new MockStateContainer());


    @Before
    public void setup(){
        integerSettableState.setEngine(mockEngine);
        stringSettableState.setEngine(mockEngine);
        integerSettableState.setValue(val1);
        stringSettableState.setValue(val2);
    }

    @After
    public void teardown(){
        integerSettableState = null;
        stringSettableState = null;
    }

    @Test
    public void getValue(){
        assert(integerSettableState.getValue() == val1);
        assert(stringSettableState.getValue() == val2);
    }

    @Test
    public void getName(){
        assert(integerSettableState.getName() == name1);
        assert(stringSettableState.getName() == name2);
    }






}
